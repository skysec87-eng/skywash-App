package com.skywash.api.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.config.ApiException;

@Service
public class GeocodeService {

  private static final Logger log = LoggerFactory.getLogger(GeocodeService.class);
  private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String googleMapsApiKey;

  public GeocodeService(
      ObjectMapper objectMapper,
      @Value("${google.maps.api-key:}") String googleMapsApiKey
  ) {
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    this.googleMapsApiKey = googleMapsApiKey == null ? "" : googleMapsApiKey.trim();
  }

  /** Package-private for unit tests without Spring. */
  GeocodeService() {
    this(new ObjectMapper(), "");
  }

  public Map<String, Object> geocode(String address) {
    if (address == null || address.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "address is required");
    }
    String normalized = address.trim();
    if (StringUtils.hasText(googleMapsApiKey)) {
      return googleGeocode("address=" + encode(normalized));
    }
    return demoGeocode(normalized);
  }

  public Map<String, Object> reverse(double lat, double lng) {
    if (Double.isNaN(lat) || Double.isNaN(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "valid lat and lng are required");
    }
    if (StringUtils.hasText(googleMapsApiKey)) {
      return googleGeocode("latlng=" + lat + "," + lng);
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("lat", round4(lat));
    out.put("lng", round4(lng));
    out.put("formatted_address", "Current location (" + round4(lat) + ", " + round4(lng) + ")");
    out.put("demo", true);
    return out;
  }

  private Map<String, Object> googleGeocode(String query) {
    try {
      URI uri = URI.create(GEOCODE_URL + "?" + query + "&key=" + encode(googleMapsApiKey));
      HttpRequest request = HttpRequest.newBuilder(uri)
          .timeout(Duration.ofSeconds(12))
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.error("Google Geocoding HTTP {}", response.statusCode());
        throw new ApiException(HttpStatus.BAD_GATEWAY, "Geocoding request failed");
      }
      JsonNode root = objectMapper.readTree(response.body());
      String status = root.path("status").asText("");
      if ("ZERO_RESULTS".equals(status)) {
        throw new ApiException(HttpStatus.NOT_FOUND, "No location found for that address");
      }
      if (!"OK".equals(status)) {
        String err = root.path("error_message").asText(status);
        log.error("Google Geocoding status {}: {}", status, err);
        throw new ApiException(HttpStatus.BAD_GATEWAY, "Geocoding failed: " + status);
      }
      JsonNode first = root.path("results").path(0);
      if (first.isMissingNode() || first.isNull()) {
        throw new ApiException(HttpStatus.NOT_FOUND, "No location found for that address");
      }
      JsonNode loc = first.path("geometry").path("location");
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("lat", loc.path("lat").asDouble());
      out.put("lng", loc.path("lng").asDouble());
      out.put("formatted_address", first.path("formatted_address").asText(""));
      out.put("demo", false);
      out.put("provider", "google");
      return out;
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Google Geocoding error: {}", ex.getMessage());
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not resolve that location");
    }
  }

  /** Deterministic demo geocoder when GOOGLE_MAPS_API_KEY is unset. */
  private Map<String, Object> demoGeocode(String normalized) {
    String lower = normalized.toLowerCase(Locale.ROOT);

    double baseLat = 6.5244;
    double baseLng = 3.3792;
    String city = "Lagos";
    String country = "Nigeria";
    // Country / city first — never rewrite Cotonou into Lagos.
    if (isCotonouBenin(lower)) {
      baseLat = 6.3654;
      baseLng = 2.4280;
      city = "Cotonou";
      country = "Benin";
    } else if (lower.contains("abuja") || lower.contains("wuse") || lower.contains("maitama") || lower.contains("garki")) {
      baseLat = 9.0765; baseLng = 7.3986; city = "Abuja";
    } else if (lower.contains("port harcourt") || lower.contains("rumuola") || lower.contains("trans amadi")) {
      baseLat = 4.8156; baseLng = 7.0498; city = "Port Harcourt";
    } else if (lower.contains("sangotedo") || lower.contains("lekki") || lower.contains("ajah")) {
      baseLat = 6.4390; baseLng = 3.5050; city = "Lagos";
    }

    CRC32 crc = new CRC32();
    crc.update(normalized.getBytes(StandardCharsets.UTF_8));
    long v = crc.getValue();
    double jitterLat = ((v % 2000) / 2000.0 - 0.5) * 0.04;
    double jitterLng = (((v / 2000) % 2000) / 2000.0 - 0.5) * 0.04;

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("lat", round4(baseLat + jitterLat));
    out.put("lng", round4(baseLng + jitterLng));
    out.put("formatted_address", formatDemoAddress(normalized, city, country));
    out.put("demo", true);
    return out;
  }

  /** Cotonou / Benin Republic — not Nigerian Benin City. */
  static boolean isCotonouBenin(String lower) {
    if (lower == null || lower.isBlank()) return false;
    if (lower.contains("benin city")) return false;
    return lower.contains("cotonou")
        || lower.contains("benin republic")
        || lower.contains("porto-novo")
        || lower.contains("porto novo")
        || lower.contains("sike codji")
        || lower.contains("rue marina")
        || lower.contains("fidjrosse")
        || lower.contains("fidjrossè")
        || (lower.contains("benin") && !lower.contains("nigeria"));
  }

  /** Keep the typed place; only append city/country when missing. */
  static String formatDemoAddress(String normalized, String city, String country) {
    String cleaned = normalized == null ? "" : normalized.trim();
    if ("Benin".equalsIgnoreCase(country)) {
      cleaned = cleaned.replaceAll("(?i),\\s*lagos\\s*,\\s*nigeria\\s*$", "").trim();
      cleaned = cleaned.replaceAll("(?i),\\s*nigeria\\s*$", "").trim();
    }
    String lower = cleaned.toLowerCase(Locale.ROOT);
    boolean hasCity = city != null && lower.contains(city.toLowerCase(Locale.ROOT));
    boolean hasCountry = country != null && lower.contains(country.toLowerCase(Locale.ROOT));
    if (hasCity && hasCountry) return cleaned;
    if (hasCity) return cleaned + ", " + country;
    if (hasCountry) return cleaned;
    return cleaned + ", " + city + ", " + country;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static double round4(double v) {
    return Math.round(v * 10000.0) / 10000.0;
  }
}
