package com.skywash.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

  @Test
  void haversineSamePointIsZero() {
    assertEquals(0.0, GeoUtils.haversineKm(6.45, 3.47, 6.45, 3.47), 1e-9);
  }

  @Test
  void haversineLagosToNearbyIsSmall() {
    double km = GeoUtils.haversineKm(6.45, 3.47, 6.450511, 3.4704056);
    assertTrue(km < 1.0, "expected nearby Lekki points under 1km, got " + km);
  }

  @Test
  void haversineLagosToAbujaIsHundredsOfKm() {
    double km = GeoUtils.haversineKm(6.5244, 3.3792, 9.0765, 7.3986);
    assertTrue(km > 400 && km < 700, "expected Lagos–Abuja ~500km, got " + km);
  }

  @Test
  void etaMinutesUsesRealisticPlannedTravelFloor() {
    assertEquals(25, GeoUtils.etaMinutes(0));
    assertEquals(25, GeoUtils.etaMinutes(1.0));
    assertEquals(45, GeoUtils.etaMinutes(5.0)); // 5*6+15
  }
}
