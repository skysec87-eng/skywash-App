package com.skywash.api.util;

public final class GeoUtils {
  private GeoUtils() {}

  /** Great-circle distance in km (matches frontend haversine). */
  public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
    final double R = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  /** One-way travel ETA (pickup). Prefer {@link TripEtaCalculator} for full trips. */
  public static int etaMinutes(double distanceKm) {
    return TripEtaCalculator.travelMinutes(distanceKm);
  }
}
