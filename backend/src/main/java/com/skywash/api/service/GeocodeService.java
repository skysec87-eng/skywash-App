package com.skywash.api.service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.skywash.api.config.ApiException;

@Service
public class GeocodeService {

  /** Deterministic demo geocoder — stable coords for a given address string. */
  public Map<String, Object> geocode(String address) {
    if (address == null || address.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "address is required");
    }
    String normalized = address.trim();
    String lower = normalized.toLowerCase(Locale.ROOT);

    // Known city anchors
    double baseLat = 6.5244;
    double baseLng = 3.3792;
    String city = "Lagos";
    if (lower.contains("abuja") || lower.contains("wuse") || lower.contains("maitama") || lower.contains("garki")) {
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
    out.put("formatted_address", normalized + ", " + city + ", Nigeria");
    out.put("demo", true);
    return out;
  }

  private static double round4(double v) {
    return Math.round(v * 10000.0) / 10000.0;
  }
}
