package com.skywash.api.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.repo.PartnerRepository;

/**
 * Live local laundry discovery — OpenStreetMap Overpass, plus Google Places when keyed.
 * Pickup in Ibadan, London, or Cotonou sees shops around that pin, never a hardcoded city list.
 */
@Service
public class LaundryDiscoveryService {

  private static final Logger log = LoggerFactory.getLogger(LaundryDiscoveryService.class);
  private static final String[] OVERPASS_URLS = {
      "https://overpass-api.de/api/interpreter",
      "https://overpass.kumi.systems/api/interpreter"
  };
  private static final String PLACES_NEARBY = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
  private static final double[] RING_KM = {4, 12, 25};
  public static final double MAX_LOCAL_KM = 25;
  private static final Pattern CAR_WASH = Pattern.compile("car\\s*wash|auto\\s*wash|vehicle\\s*wash", Pattern.CASE_INSENSITIVE);

  private final PartnerRepository partnerRepository;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final boolean enabled;
  private final String googleMapsApiKey;

  @Autowired
  public LaundryDiscoveryService(
      PartnerRepository partnerRepository,
      ObjectMapper objectMapper,
      @Value("${skywash.discovery.overpass:true}") boolean enabled,
      @Value("${google.maps.api-key:}") String googleMapsApiKey
  ) {
    this.partnerRepository = partnerRepository;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    this.enabled = enabled;
    this.googleMapsApiKey = googleMapsApiKey == null ? "" : googleMapsApiKey.trim();
    log.info(
        "Local laundry discovery {}",
        enabled
            ? (StringUtils.hasText(this.googleMapsApiKey) ? "ON (OpenStreetMap + Google Places)" : "ON (OpenStreetMap)")
            : "OFF"
    );
  }

  /** Tests — discovery off, no HTTP. */
  LaundryDiscoveryService() {
    this(null, new ObjectMapper(), false, "");
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** Pull live laundries around pickup and upsert so they can be booked. */
  public double ingestAround(double lat, double lng, int want) {
    if (!enabled || partnerRepository == null) return MAX_LOCAL_KM;
    int need = Math.max(3, want);
    int found = 0;
    if (StringUtils.hasText(googleMapsApiKey)) {
      found += upsertAll(fetchGooglePlaces(lat, lng, MAX_LOCAL_KM));
      if (found >= need) return MAX_LOCAL_KM;
    }
    double usedKm = RING_KM[0];
    for (double km : RING_KM) {
      usedKm = km;
      found += upsertAll(fetchOsmRing(lat, lng, km, found < need));
      if (found >= need) return km;
    }
    return usedKm;
  }

  private int upsertAll(List<Map<String, Object>> places) {
    int n = 0;
    for (Map<String, Object> place : places) {
      if (place == null) continue;
      upsert(place);
      n++;
    }
    return n;
  }

  private List<Map<String, Object>> fetchOsmRing(double lat, double lng, double km, boolean alsoNames) {
    List<Map<String, Object>> tagged = safeOverpass(taggedQuery(lat, lng, km), km);
    if (!alsoNames || tagged.size() >= 3) return tagged;
    List<Map<String, Object>> named = safeOverpass(namedQuery(lat, lng, km), km);
    if (named.isEmpty()) return tagged;
    List<Map<String, Object>> merged = new ArrayList<>(tagged);
    for (Map<String, Object> place : named) {
      String id = String.valueOf(place.get("id"));
      boolean dup = merged.stream().anyMatch(p -> id.equals(String.valueOf(p.get("id"))));
      if (!dup) merged.add(place);
    }
    return merged;
  }

  private List<Map<String, Object>> safeOverpass(String query, double km) {
    try {
      return postOverpass(query);
    } catch (Exception ex) {
      log.warn("Overpass laundry search failed at {}km: {}", km, ex.getMessage());
      return List.of();
    }
  }

  private String taggedQuery(double lat, double lng, double km) {
    return """
        [out:json][timeout:18];
        (
          nwr["shop"="laundry"](around:%d,%s,%s);
          nwr["amenity"="laundry"](around:%d,%s,%s);
          nwr["shop"="dry_cleaning"](around:%d,%s,%s);
          nwr["amenity"="dry_cleaning"](around:%d,%s,%s);
          nwr["shop"="dry_cleaner"](around:%d,%s,%s);
        );
        out center tags 80;
        """.formatted(
        meters(km), lat, lng,
        meters(km), lat, lng,
        meters(km), lat, lng,
        meters(km), lat, lng,
        meters(km), lat, lng
    ).trim();
  }

  private String namedQuery(double lat, double lng, double km) {
    return """
        [out:json][timeout:18];
        (
          nwr["name"~"laundry|laundromat|pressing|launderette|dry.?clean",i](around:%d,%s,%s);
        );
        out center tags 80;
        """.formatted(meters(km), lat, lng).trim();
  }

  private List<Map<String, Object>> postOverpass(String query) throws Exception {
    String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    Exception last = null;
    for (String url : OVERPASS_URLS) {
      try {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(8))
            .header("User-Agent", "skyWash/1.0 (https://sudsnear-deploy.vercel.app)")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          throw new IllegalStateException("Overpass HTTP " + response.statusCode() + " from " + url);
        }
        return parseElements(objectMapper.readTree(response.body()));
      } catch (Exception ex) {
        last = ex;
        log.warn("Overpass failed: {}", ex.getMessage());
      }
    }
    throw last != null ? last : new IllegalStateException("Overpass unavailable");
  }

  private List<Map<String, Object>> fetchGooglePlaces(double lat, double lng, double km) {
    try {
      String q = "laundry OR pressing OR \"dry cleaner\" OR laundromat";
      URI uri = URI.create(
          PLACES_NEARBY
              + "?location=" + lat + "," + lng
              + "&radius=" + meters(km)
              + "&keyword=" + encode(q)
              + "&key=" + encode(googleMapsApiKey)
      );
      HttpRequest request = HttpRequest.newBuilder(uri)
          .timeout(Duration.ofSeconds(12))
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("Google Places HTTP {}", response.statusCode());
        return List.of();
      }
      JsonNode root = objectMapper.readTree(response.body());
      String status = root.path("status").asText("");
      if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
        log.warn("Google Places status {}: {}", status, root.path("error_message").asText(""));
        return List.of();
      }
      return parseGooglePlaces(root);
    } catch (Exception ex) {
      log.warn("Google Places nearby failed: {}", ex.getMessage());
      return List.of();
    }
  }

  static List<Map<String, Object>> parseElements(JsonNode root) {
    List<Map<String, Object>> out = new ArrayList<>();
    if (root == null || !root.has("elements")) return out;
    for (JsonNode el : root.path("elements")) {
      Map<String, Object> place = fromElement(el);
      if (place != null) out.add(place);
    }
    return out;
  }

  static List<Map<String, Object>> parseGooglePlaces(JsonNode root) {
    List<Map<String, Object>> out = new ArrayList<>();
    if (root == null || !root.has("results")) return out;
    for (JsonNode el : root.path("results")) {
      Map<String, Object> place = fromGooglePlace(el);
      if (place != null) out.add(place);
    }
    return out;
  }

  static Map<String, Object> fromElement(JsonNode el) {
    if (el == null || el.isMissingNode()) return null;
    double lat;
    double lng;
    if (el.has("lat") && el.has("lon")) {
      lat = el.path("lat").asDouble();
      lng = el.path("lon").asDouble();
    } else if (el.path("center").has("lat")) {
      lat = el.path("center").path("lat").asDouble();
      lng = el.path("center").path("lon").asDouble();
    } else {
      return null;
    }
    JsonNode tags = el.path("tags");
    String name = firstTag(tags, "name", "name:en", "brand");
    if (!StringUtils.hasText(name)) {
      name = "Local laundry";
    }
    if (skipPlace(name)) return null;
    String city = firstTag(tags, "addr:city", "addr:province", "is_in:city");
    if (!StringUtils.hasText(city)) city = "Nearby";
    String area = firstTag(tags, "addr:suburb", "addr:quarter", "addr:neighbourhood", "addr:district");
    if (!StringUtils.hasText(area)) area = city;
    String street = firstTag(tags, "addr:street", "addr:place");
    String housenum = firstTag(tags, "addr:housenumber");
    String address = StringUtils.hasText(street)
        ? ((StringUtils.hasText(housenum) ? housenum + " " : "") + street + ", " + city)
        : (name + ", " + city);
    String phone = firstTag(tags, "phone", "contact:phone");
    String type = el.path("type").asText("node");
    long osmId = el.path("id").asLong();
    if (osmId == 0) return null;

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", "osm-" + type + "-" + osmId);
    m.put("name", name);
    m.put("city", city);
    m.put("area", area);
    m.put("address", address);
    m.put("lat", lat);
    m.put("lng", lng);
    m.put("phone", phone);
    m.put("rating", ratingFor(osmId));
    return m;
  }

  static Map<String, Object> fromGooglePlace(JsonNode el) {
    if (el == null || el.isMissingNode()) return null;
    String placeId = el.path("place_id").asText("");
    String name = el.path("name").asText("");
    if (!StringUtils.hasText(placeId) || !StringUtils.hasText(name) || skipPlace(name)) return null;
    JsonNode loc = el.path("geometry").path("location");
    if (!loc.has("lat") || !loc.has("lng")) return null;
    String vicinity = el.path("vicinity").asText(name);
    double rating = el.path("rating").isNumber() ? el.path("rating").asDouble() : ratingFor(placeId.hashCode());
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", "gplace-" + placeId);
    m.put("name", name);
    m.put("city", "Nearby");
    m.put("area", "Nearby");
    m.put("address", vicinity);
    m.put("lat", loc.path("lat").asDouble());
    m.put("lng", loc.path("lng").asDouble());
    m.put("phone", "");
    m.put("rating", Math.round(rating * 10.0) / 10.0);
    return m;
  }

  static boolean skipPlace(String name) {
    return name != null && CAR_WASH.matcher(name).find();
  }

  private void upsert(Map<String, Object> place) {
    String id = String.valueOf(place.get("id"));
    if (id.length() > 190) id = id.substring(0, 190);
    PartnerEntity e = partnerRepository.findById(id).orElseGet(PartnerEntity::new);
    e.setId(id);
    e.setName(String.valueOf(place.get("name")));
    e.setCity(String.valueOf(place.get("city")));
    e.setArea(String.valueOf(place.get("area")));
    e.setAddress(String.valueOf(place.get("address")));
    e.setLat(((Number) place.get("lat")).doubleValue());
    e.setLng(((Number) place.get("lng")).doubleValue());
    e.setRating(((Number) place.get("rating")).doubleValue());
    Object phone = place.get("phone");
    if (phone != null && StringUtils.hasText(String.valueOf(phone))) {
      e.setPhone(String.valueOf(phone));
    }
    if (!StringUtils.hasText(e.getEmail())) {
      String slug = id.replace("osm-", "").replace("gplace-", "");
      if (slug.length() > 40) slug = slug.substring(0, 40);
      e.setEmail("ops+" + slug + "@partner.skywash.app");
    }
    e.setActive(true);
    partnerRepository.save(e);
  }

  static double ratingFor(long osmId) {
    CRC32 crc = new CRC32();
    crc.update(Long.toString(osmId).getBytes(StandardCharsets.UTF_8));
    int step = (int) (crc.getValue() % 8);
    return Math.round((4.2 + step * 0.1) * 10.0) / 10.0;
  }

  private static int meters(double km) {
    return (int) Math.round(km * 1000);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static String firstTag(JsonNode tags, String... keys) {
    if (tags == null || tags.isMissingNode()) return null;
    for (String k : keys) {
      String v = tags.path(k).asText(null);
      if (StringUtils.hasText(v)) return v.trim();
    }
    return null;
  }
}
