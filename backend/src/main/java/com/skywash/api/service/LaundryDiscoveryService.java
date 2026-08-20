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
import java.util.Locale;
import java.util.Map;
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
 * Live local laundry discovery from OpenStreetMap (Overpass).
 * Cotonou sees Cotonou shops; a US pickup sees US shops — never a far-away seed city.
 */
@Service
public class LaundryDiscoveryService {

  private static final Logger log = LoggerFactory.getLogger(LaundryDiscoveryService.class);
  /** Public Overpass mirrors — dense cities like London often 504 the main endpoint. */
  private static final String[] OVERPASS_URLS = {
      "https://overpass-api.de/api/interpreter",
      "https://lz4.overpass-api.de/api/interpreter",
      "https://overpass.kumi.systems/api/interpreter"
  };
  /** Tight first ring so London/NYC queries finish; expand only if sparse. */
  private static final double[] RING_KM = {2, 8, 20};
  public static final double MAX_LOCAL_KM = 20;

  private final PartnerRepository partnerRepository;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final boolean enabled;

  @Autowired
  public LaundryDiscoveryService(
      PartnerRepository partnerRepository,
      ObjectMapper objectMapper,
      @Value("${skywash.discovery.overpass:true}") boolean enabled
  ) {
    this.partnerRepository = partnerRepository;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    this.enabled = enabled;
    log.info("Local laundry discovery {}", enabled ? "ON (OpenStreetMap)" : "OFF");
  }

  /** Tests — discovery off, no HTTP. */
  LaundryDiscoveryService() {
    this(null, new ObjectMapper(), false);
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** Pull OSM laundries around pickup and upsert so they can be booked. */
  public double ingestAround(double lat, double lng, int want) {
    if (!enabled || partnerRepository == null) return MAX_LOCAL_KM;
    int need = Math.max(3, want);
    double usedKm = RING_KM[0];
    for (double km : RING_KM) {
      usedKm = km;
      try {
        List<Map<String, Object>> places = fetchRing(lat, lng, km);
        for (Map<String, Object> place : places) {
          upsert(place);
        }
        long local = places.size();
        if (local >= need || km >= RING_KM[RING_KM.length - 1]) {
          return km;
        }
      } catch (Exception ex) {
        log.warn("Overpass laundry search failed at {}km: {}", km, ex.getMessage());
      }
    }
    return usedKm;
  }

  List<Map<String, Object>> fetchRing(double lat, double lng, double km) throws Exception {
    String query = """
        [out:json][timeout:18];
        (
          nwr["shop"="laundry"](around:%d,%s,%s);
          nwr["amenity"="laundry"](around:%d,%s,%s);
          nwr["shop"="dry_cleaning"](around:%d,%s,%s);
          nwr["amenity"="dry_cleaning"](around:%d,%s,%s);
        );
        out center tags 80;
        """.formatted(
        meters(km), lat, lng,
        meters(km), lat, lng,
        meters(km), lat, lng,
        meters(km), lat, lng
    ).trim();
    String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    Exception last = null;
    for (String url : OVERPASS_URLS) {
      try {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
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
        log.warn("Overpass {}km failed: {}", km, ex.getMessage());
      }
    }
    throw last != null ? last : new IllegalStateException("Overpass unavailable");
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

  private void upsert(Map<String, Object> place) {
    String id = String.valueOf(place.get("id"));
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
      e.setEmail("ops+" + id.replace("osm-", "") + "@partner.skywash.app");
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

  private static String firstTag(JsonNode tags, String... keys) {
    if (tags == null || tags.isMissingNode()) return null;
    for (String k : keys) {
      String v = tags.path(k).asText(null);
      if (StringUtils.hasText(v)) return v.trim();
    }
    return null;
  }
}
