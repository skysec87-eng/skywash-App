package com.skywash.api.data;

import java.util.List;

import com.skywash.api.model.ServiceType;

public final class SeedData {
  private SeedData() {}

  public static final int BASE_FEE = 500;
  public static final double PLATFORM_FEE_RATE = 0.10;
  public static final double NEARBY_RADIUS_KM = 40;
  public static final int NEARBY_LIMIT = 8;

  public static List<ServiceType> services() {
    return List.of(
        new ServiceType("wash", "Wash & Fold", 500, "kg", "🧺"),
        new ServiceType("dry", "Dry Cleaning", 1500, "item", "🧥"),
        new ServiceType("iron", "Iron Only", 300, "kg", "👔"),
        new ServiceType("express", "Express (4h)", 900, "kg", "⚡")
    );
  }

  /**
   * Pilot partners for Lagos when Neon is empty / OSM discovery returns nothing.
   * Nearby still enforces the 25 km cap — pickups must be in Lagos to see these.
   */
  public static List<PilotPartner> pilotPartners() {
    return List.of(
        new PilotPartner("seed-yaba-fresh", "Yaba Fresh Laundry", "Lagos", "Yaba",
            "Herbert Macaulay Way, Yaba, Lagos", 6.5095, 3.3860, 4.8, "+2348010000001"),
        new PilotPartner("seed-surulere-clean", "Surulere Clean Co", "Lagos", "Surulere",
            "Adeniran Ogunsanya Street, Surulere, Lagos", 6.4969, 3.3566, 4.6, "+2348010000002"),
        new PilotPartner("seed-vi-express", "VI Express Wash", "Lagos", "Victoria Island",
            "Adeola Odeku Street, Victoria Island, Lagos", 6.4281, 3.4219, 4.9, "+2348010000003"),
        new PilotPartner("seed-lekki-fold", "Lekki Fold & Go", "Lagos", "Lekki Phase 1",
            "Admiralty Way, Lekki Phase 1, Lagos", 6.4474, 3.4722, 4.5, "+2348010000004"),
        new PilotPartner("seed-ikeja-spark", "Ikeja Spark Wash", "Lagos", "Ikeja",
            "Allen Avenue, Ikeja, Lagos", 6.6018, 3.3515, 4.7, "+2348010000005"),
        new PilotPartner("seed-mainland-care", "Mainland Care Laundry", "Lagos", "Somolu",
            "Market Street, Somolu, Lagos", 6.5408, 3.3842, 4.4, "+2348010000006")
    );
  }

  public static final List<StatusDef> STATUSES = List.of(
      new StatusDef("confirmed", "Request confirmed", 3_000),
      new StatusDef("enroute", "Partner heading to you", 6_000),
      new StatusDef("pickedup", "Picked up from you", 3_000),
      new StatusDef("washing", "Washing at the laundromat", 7_000),
      new StatusDef("delivering", "Out for delivery", 6_000),
      new StatusDef("delivered", "Delivered", 0)
  );

  public record StatusDef(String key, String label, long durationMs) {}

  public record PilotPartner(
      String id,
      String name,
      String city,
      String area,
      String address,
      double lat,
      double lng,
      double rating,
      String phone
  ) {}
}
