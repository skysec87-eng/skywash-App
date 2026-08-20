package com.skywash.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class TripEtaCalculatorTest {

  @Test
  void plannedTravelNeverPromisesSub25Minutes() {
    assertEquals(25, TripEtaCalculator.travelMinutes(0));
    assertEquals(25, TripEtaCalculator.travelMinutes(1.0)); // 6+15=21 → floor 25
    assertEquals(45, TripEtaCalculator.travelMinutes(5.0)); // 30+15=45
  }

  @Test
  void liveTravelCanBeShortWhenRiderIsClose() {
    assertTrue(TripEtaCalculator.travelMinutes(0.2, false) < 25);
    assertTrue(TripEtaCalculator.travelMinutes(0.2, false) >= TripEtaCalculator.MIN_LIVE_TRAVEL_MIN);
  }

  @Test
  void washAndFoldIsMultiHourNotMinutes() {
    assertTrue(TripEtaCalculator.washMinutesFor("wash", 3) >= 150);
    assertTrue(TripEtaCalculator.washMinutesFor("dry", 1) >= 300);
    assertTrue(TripEtaCalculator.washMinutesFor("iron", 1) >= 75);
  }

  @Test
  void nearbyWashTripIsHoursNotMinutes() {
    var plan = TripEtaCalculator.plan(2.0, "wash", 3);
    // travel ≥25 each way + 15 handover + ~150 wash + 20 dispatch
    assertTrue(plan.pickupTravelMin() >= 25);
    assertEquals(15, plan.pickupHandlingMin());
    assertEquals(20, plan.dispatchHandlingMin());
    assertTrue(plan.washMin() >= 150);
    assertTrue(plan.totalMin() >= 200, "expected ~3h+ door-to-door, got " + plan.totalMin());
    assertEquals(
        plan.pickupTravelMin() + plan.pickupHandlingMin() + plan.washMin()
            + plan.dispatchHandlingMin() + plan.deliveryTravelMin(),
        plan.totalMin()
    );
  }

  @Test
  void expressHoldsFourHourSlaWhenNearby() {
    var plan = TripEtaCalculator.plan(1.0, "express", 2);
    assertTrue(plan.totalMin() >= 220 && plan.totalMin() <= 280, "got " + plan.totalMin());
  }

  @Test
  void remainingShrinksThroughLifecycle() {
    var plan = TripEtaCalculator.plan(2.0, "wash", 2);
    Instant now = Instant.parse("2026-08-20T12:00:00Z");
    var confirmed = TripEtaCalculator.remaining("confirmed", plan, now, now, null);
    var washing = TripEtaCalculator.remaining("washing", plan, now, now, null);
    var delivering = TripEtaCalculator.remaining("delivering", plan, now, now, 12);
    var done = TripEtaCalculator.remaining("delivered", plan, now, now, null);

    assertEquals("pickup", confirmed.phase());
    assertEquals(plan.totalMin(), confirmed.remainingMin());
    assertTrue(washing.remainingMin() < confirmed.remainingMin());
    assertEquals(12, delivering.remainingMin());
    assertEquals(0, done.remainingMin());
    assertEquals("Delivered", done.label());
  }

  @Test
  void etaLabelUsesHoursWhenLong() {
    assertEquals("~45 min", TripEtaCalculator.formatEtaLabel(45));
    assertEquals("~2h", TripEtaCalculator.formatEtaLabel(120));
    assertEquals("~2h 20m", TripEtaCalculator.formatEtaLabel(140));
  }

  @Test
  void phaseDurationScalesButStaysDemoFriendly() {
    var near = TripEtaCalculator.plan(0.5, "wash", 1);
    var far = TripEtaCalculator.plan(8.0, "wash", 1);
    long nearMs = TripEtaCalculator.phaseDurationMs("enroute", near);
    long farMs = TripEtaCalculator.phaseDurationMs("enroute", far);
    assertTrue(farMs >= nearMs);
    assertTrue(nearMs >= TripEtaCalculator.DEMO_PHASE_MIN_MS);
    assertTrue(farMs <= TripEtaCalculator.DEMO_PHASE_MAX_MS);
  }

  @Test
  void mixedServicesAddSurcharge() {
    int single = TripEtaCalculator.washMinutes(List.of(new TripEtaCalculator.ServiceQty("wash", 2)));
    int mixed = TripEtaCalculator.washMinutes(List.of(
        new TripEtaCalculator.ServiceQty("wash", 2),
        new TripEtaCalculator.ServiceQty("iron", 2)
    ));
    assertTrue(mixed >= single + 30);
  }
}
