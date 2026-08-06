package com.skywash.api.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.skywash.api.entity.OrderEntity;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.ServiceTypeEntity;

public final class TestFixtures {
  private TestFixtures() {}

  public static PartnerEntity partner(String id, String name, String city, String area,
                                      double lat, double lng, double rating) {
    PartnerEntity p = new PartnerEntity();
    p.setId(id);
    p.setName(name);
    p.setCity(city);
    p.setArea(area);
    p.setAddress(area + ", " + city);
    p.setLat(lat);
    p.setLng(lng);
    p.setRating(rating);
    p.setPhone("+2348000000000");
    p.setActive(true);
    return p;
  }

  public static ServiceTypeEntity service(String type, String label, int rate, String unit, String icon) {
    ServiceTypeEntity s = new ServiceTypeEntity();
    s.setType(type);
    s.setLabel(label);
    s.setRate(rate);
    s.setUnit(unit);
    s.setIcon(icon);
    return s;
  }

  public static OrderEntity order(String id, String partnerId, String status) {
    OrderEntity o = new OrderEntity();
    o.setId(id);
    o.setStatus(status);
    o.setStatusLabel(status);
    o.setPickupLat(6.45);
    o.setPickupLng(3.47);
    o.setPickupAddress("Sangotedo");
    o.setPartnerId(partnerId);
    o.setProviderName("Test Laundry");
    o.setServices(new ArrayList<>(List.of(new OrderEntity.ServiceLineEmbed("wash", 3))));
    o.setQuantity(3);
    o.setPaymentMethod("card");
    o.setPaymentDisplay("Debit / Credit Card");
    o.setBaseFee(500);
    o.setServiceCost(1500);
    o.setPlatformFee(150);
    o.setDiscount(0);
    o.setTotal(2150);
    o.setCurrency("NGN");
    o.setServiceLabel("Wash & Fold · 3kg");
    o.setCreatedAt(Instant.now());
    o.setUpdatedAt(Instant.now());
    o.setCourierLat(6.45);
    o.setCourierLng(3.47);
    o.getTimeline().add(new OrderEntity.TimelineEmbed(status, status, Instant.now()));
    return o;
  }
}
