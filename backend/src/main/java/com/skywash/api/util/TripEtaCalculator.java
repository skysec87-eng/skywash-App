package com.skywash.api.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Real-world trip timing for Nigerian on-demand laundry.
 *
 *   total = pickup_travel + door_handover + plant_time + outbound_stage + delivery_travel
 *
 * Design notes (product, not demo fantasy):
 * - Never promise sub-20 minute first arrival in Lagos traffic / dispatch.
 * - Door handover includes estate access, security, bag check — not a 5-minute tap.
 * - Wash & fold is multi-hour plant work; dry cleaning longer; express is an SLA (~4h).
 * - Live courier ETAs may drop below the planning floor once the rider is already en route.
 */
public final class TripEtaCalculator {

  /** Effective urban speed ≈ 10 km/h (okada/keke + traffic). */
  public static final double MIN_PER_KM = 6.0;
  /** Assign rider + leave shop buffer on a planned leg. */
  public static final int TRAVEL_BUFFER_MIN = 15;
  /** Hard floor for planned shop→customer (or return) legs. */
  public static final int MIN_PLANNED_TRAVEL_MIN = 25;
  /** Estate / security / bag exchange at pickup. */
  public static final int PICKUP_HANDLING_MIN = 15;
  /** Sort, QC, bag, stage for outbound rider. */
  public static final int DISPATCH_HANDLING_MIN = 20;
  /** Last-meter handoff when rider is already at the door. */
  public static final int MIN_LIVE_TRAVEL_MIN = 8;

  /** Express door-to-door SLA (minutes). */
  public static final int EXPRESS_SLA_MIN = 240;

  /**
   * Demo accelerator only — UI still shows real minutes.
   * 1 planned minute ≈ this many ms on the auto-progress scheduler.
   */
  public static final long DEMO_MS_PER_REAL_MIN = 180L;
  public static final long DEMO_PHASE_MIN_MS = 4_000L;
  public static final long DEMO_PHASE_MAX_MS = 35_000L;

  private TripEtaCalculator() {}

  public record ServiceQty(String type, int qty) {
    public ServiceQty {
      type = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
      qty = Math.max(1, qty);
    }
  }

  public record Plan(
      double distanceKm,
      int pickupTravelMin,
      int pickupHandlingMin,
      int washMin,
      int dispatchHandlingMin,
      int deliveryTravelMin,
      int totalMin
  ) {
    public Map<String, Object> toMap() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("distance_km", Math.round(distanceKm * 10.0) / 10.0);
      m.put("pickup_min", pickupTravelMin + pickupHandlingMin);
      m.put("pickup_travel_min", pickupTravelMin);
      m.put("pickup_handling_min", pickupHandlingMin);
      m.put("wash_min", washMin);
      m.put("delivery_min", dispatchHandlingMin + deliveryTravelMin);
      m.put("dispatch_handling_min", dispatchHandlingMin);
      m.put("delivery_travel_min", deliveryTravelMin);
      m.put("total_min", totalMin);
      return m;
    }
  }

  public record Remaining(
      String phase,
      int remainingMin,
      String label,
      Plan plan
  ) {
    public Map<String, Object> toMap() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("phase", phase);
      m.put("remaining_min", remainingMin);
      m.put("label", label);
      m.put("breakdown", plan.toMap());
      return m;
    }
  }

  /** Planned one-way travel (shop ↔ customer). */
  public static int travelMinutes(double distanceKm) {
    return travelMinutes(distanceKm, true);
  }

  /**
   * @param plannedLeg true for shop dispatch planning; false for live remaining distance
   */
  public static int travelMinutes(double distanceKm, boolean plannedLeg) {
    double d = Math.max(0, distanceKm);
    if (plannedLeg) {
      int raw = (int) Math.round(d * MIN_PER_KM + TRAVEL_BUFFER_MIN);
      return Math.max(MIN_PLANNED_TRAVEL_MIN, raw);
    }
    // Already moving: short leftover distance can be minutes, not a new 25m dispatch.
    int raw = (int) Math.round(d * MIN_PER_KM + 3);
    return Math.max(MIN_LIVE_TRAVEL_MIN, raw);
  }

  /** Plant / processing time. Mixed bags take the slower service + surcharge. */
  public static int washMinutes(List<ServiceQty> services) {
    if (services == null || services.isEmpty()) {
      return washMinutesFor("wash", 2);
    }
    int max = 0;
    boolean mixed = false;
    String first = null;
    for (ServiceQty s : services) {
      int m = washMinutesFor(s.type(), s.qty());
      max = Math.max(max, m);
      if (first == null) first = s.type();
      else if (!first.equals(s.type())) mixed = true;
    }
    if (mixed) max += 30;
    return Math.max(60, max);
  }

  static int washMinutesFor(String type, int qty) {
    int q = Math.max(1, qty);
    return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
      // Express plant time is finalized in plan() against the 4h SLA.
      case "express" -> 120 + 15 * Math.max(0, q - 2);
      case "dry" -> 300 + 45 * Math.max(0, q - 1);      // ~5h+ dry-clean cycle
      case "iron" -> 75 + 12 * Math.max(0, q - 1);       // ~1–2h press line
      case "wash" -> 150 + 20 * Math.max(0, q - 3);      // ~2.5h wash & fold base
      default -> 150 + 20 * Math.max(0, q - 3);
    };
  }

  public static Plan plan(double distanceKm, List<ServiceQty> services) {
    int travel = travelMinutes(distanceKm, true);
    int wash = washMinutes(services);
    boolean express = services != null && services.stream().anyMatch(s -> "express".equals(s.type()));
    if (express) {
      int fixed = travel + PICKUP_HANDLING_MIN + DISPATCH_HANDLING_MIN + travel;
      // Hold a 4h door-to-door promise when logistics allow; never under-cook plant time.
      wash = Math.max(wash, Math.max(90, EXPRESS_SLA_MIN - fixed));
    }
    int total = travel + PICKUP_HANDLING_MIN + wash + DISPATCH_HANDLING_MIN + travel;
    return new Plan(
        distanceKm,
        travel,
        PICKUP_HANDLING_MIN,
        wash,
        DISPATCH_HANDLING_MIN,
        travel,
        total
    );
  }

  public static Plan plan(double distanceKm, String serviceType, int qty) {
    return plan(distanceKm, List.of(new ServiceQty(serviceType, qty)));
  }

  public static Remaining remaining(
      String status,
      Plan plan,
      Instant phaseEntered,
      Instant now,
      Integer liveTravelMin
  ) {
    if (status == null) status = "";
    String s = status.toLowerCase(Locale.ROOT);
    if ("cancelled".equals(s)) {
      return new Remaining("done", 0, "—", plan);
    }
    if ("delivered".equals(s) || "rated".equals(s)) {
      return new Remaining("done", 0, "Delivered", plan);
    }

    long elapsedMin = 0;
    if (phaseEntered != null && now != null && now.isAfter(phaseEntered)) {
      elapsedMin = Math.max(0, Duration.between(phaseEntered, now).toMinutes());
    }

    int rem;
    String phase;
    switch (s) {
      case "confirmed" -> {
        phase = "pickup";
        rem = plan.totalMin();
        rem = credit(rem, elapsedMin);
      }
      case "enroute" -> {
        phase = "pickup";
        int toPickup = liveTravelMin != null ? liveTravelMin : plan.pickupTravelMin();
        rem = toPickup + plan.pickupHandlingMin()
            + plan.washMin() + plan.dispatchHandlingMin() + plan.deliveryTravelMin();
      }
      case "pickedup" -> {
        phase = "wash";
        int toPlant = liveTravelMin != null ? liveTravelMin : plan.pickupTravelMin();
        rem = toPlant + plan.washMin() + plan.dispatchHandlingMin() + plan.deliveryTravelMin();
        rem = credit(rem, elapsedMin);
      }
      case "washing" -> {
        phase = "wash";
        rem = credit(plan.washMin(), elapsedMin)
            + plan.dispatchHandlingMin() + plan.deliveryTravelMin();
      }
      case "delivering" -> {
        phase = "delivery";
        int toCustomer = liveTravelMin != null ? liveTravelMin : plan.deliveryTravelMin();
        rem = Math.max(MIN_LIVE_TRAVEL_MIN, toCustomer);
      }
      default -> {
        phase = "pickup";
        rem = plan.totalMin();
      }
    }

    rem = Math.max(0, rem);
    return new Remaining(phase, rem, formatEtaLabel(rem), plan);
  }

  public static Remaining scheduled(Plan plan) {
    return new Remaining("scheduled", plan.totalMin(), "Scheduled", plan);
  }

  /** Human ETA copy: "~45 min", "~2h", "~2h 20m". */
  public static String formatEtaLabel(int minutes) {
    if (minutes <= 0) return "Soon";
    if (minutes < 60) return "~" + minutes + " min";
    int h = minutes / 60;
    int m = minutes % 60;
    if (m == 0) return "~" + h + "h";
    if (h >= 3 && m < 15) return "~" + h + "h";
    return "~" + h + "h " + m + "m";
  }

  /** Demo scheduler only — does not change customer-facing minutes. */
  public static long phaseDurationMs(String status, Plan plan) {
    if (status == null) return DEMO_PHASE_MIN_MS;
    int mins = switch (status.toLowerCase(Locale.ROOT)) {
      case "confirmed" -> Math.max(8, Math.min(20, plan.pickupHandlingMin()));
      case "enroute" -> plan.pickupTravelMin();
      case "pickedup" -> Math.max(10, Math.min(plan.pickupTravelMin(), 30));
      case "washing" -> plan.washMin();
      case "delivering" -> plan.deliveryTravelMin();
      default -> 15;
    };
    long ms = mins * DEMO_MS_PER_REAL_MIN;
    return Math.max(DEMO_PHASE_MIN_MS, Math.min(DEMO_PHASE_MAX_MS, ms));
  }

  public static List<ServiceQty> fromMaps(List<Map<String, Object>> services) {
    List<ServiceQty> out = new ArrayList<>();
    if (services == null) return out;
    for (Map<String, Object> s : services) {
      if (s == null) continue;
      Object type = s.get("type");
      Object qty = s.get("qty");
      int q = 1;
      if (qty instanceof Number n) q = n.intValue();
      else if (qty != null) {
        try { q = Integer.parseInt(String.valueOf(qty)); } catch (NumberFormatException ignored) {}
      }
      out.add(new ServiceQty(type == null ? "wash" : String.valueOf(type), q));
    }
    return out;
  }

  public interface TypeQty {
    String type();
    int qty();
  }

  private static int credit(int planned, long elapsedMin) {
    return (int) Math.max(1, planned - elapsedMin);
  }
}
