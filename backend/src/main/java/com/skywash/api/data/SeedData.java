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

  public static final List<StatusDef> STATUSES = List.of(
      new StatusDef("confirmed", "Request confirmed", 3_000),
      new StatusDef("enroute", "Partner heading to you", 6_000),
      new StatusDef("pickedup", "Picked up from you", 3_000),
      new StatusDef("washing", "Washing at the laundromat", 7_000),
      new StatusDef("delivering", "Out for delivery", 6_000),
      new StatusDef("delivered", "Delivered", 0)
  );

  public record StatusDef(String key, String label, long durationMs) {}
}
