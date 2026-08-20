package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.skywash.api.config.ApiException;

class GeocodeServiceTest {

  private GeocodeService geocodeService;

  @BeforeEach
  void setUp() {
    geocodeService = new GeocodeService();
  }

  @Test
  void blankAddressThrows() {
    assertThrows(ApiException.class, () -> geocodeService.geocode("  "));
    assertThrows(ApiException.class, () -> geocodeService.geocode(null));
  }

  @Test
  void abujaAddressAnchorsNearAbuja() {
    Map<String, Object> result = geocodeService.geocode("Wuse II, Abuja");
    double lat = ((Number) result.get("lat")).doubleValue();
    double lng = ((Number) result.get("lng")).doubleValue();
    assertTrue(Math.abs(lat - 9.0765) < 0.05);
    assertTrue(Math.abs(lng - 7.3986) < 0.05);
    assertTrue(String.valueOf(result.get("formatted_address")).contains("Abuja"));
    assertEquals(true, result.get("demo"));
  }

  @Test
  void lekkiAddressAnchorsNearLagos() {
    Map<String, Object> result = geocodeService.geocode("Lekki Phase 1");
    double lat = ((Number) result.get("lat")).doubleValue();
    assertTrue(Math.abs(lat - 6.4390) < 0.05);
  }

  @Test
  void sameAddressIsDeterministic() {
    Map<String, Object> a = geocodeService.geocode("Sangotedo");
    Map<String, Object> b = geocodeService.geocode("Sangotedo");
    assertEquals(a.get("lat"), b.get("lat"));
    assertEquals(a.get("lng"), b.get("lng"));
  }

  @Test
  void cotonouIsNotRewrittenToLagosNigeria() {
    Map<String, Object> result = geocodeService.geocode(
        "Sike Codji, Rue Marina, Cotonou, Benin, Lagos, Nigeria"
    );
    String formatted = String.valueOf(result.get("formatted_address"));
    assertTrue(formatted.toLowerCase().contains("cotonou"));
    assertTrue(formatted.toLowerCase().contains("benin"));
    assertTrue(!formatted.toLowerCase().contains("nigeria"), formatted);
    double lng = ((Number) result.get("lng")).doubleValue();
    assertTrue(lng < 3.0, "Cotonou is west of Lagos; got lng=" + lng);
  }

  @Test
  void beninCityStaysInNigeria() {
    Map<String, Object> result = geocodeService.geocode("Ring Road, Benin City");
    String formatted = String.valueOf(result.get("formatted_address"));
    assertTrue(formatted.contains("Nigeria"));
  }
}
