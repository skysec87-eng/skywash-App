package com.skywash.api.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.skywash.api.config.ApiException;
import com.skywash.api.entity.OrderEntity;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.PartnerNotificationEntity;
import com.skywash.api.repo.OrderRepository;
import com.skywash.api.repo.PartnerRepository;

@Service
public class OpsDashboardService {

  private final PartnerRepository partnerRepository;
  private final OrderRepository orderRepository;
  private final PartnerNotifyService partnerNotifyService;

  public OpsDashboardService(
      PartnerRepository partnerRepository,
      OrderRepository orderRepository,
      PartnerNotifyService partnerNotifyService
  ) {
    this.partnerRepository = partnerRepository;
    this.orderRepository = orderRepository;
    this.partnerNotifyService = partnerNotifyService;
  }

  public List<Map<String, Object>> listPartners() {
    return partnerRepository.findByActiveTrue().stream()
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .map(p -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", p.getId());
          m.put("name", p.getName());
          m.put("city", p.getCity());
          m.put("area", p.getArea());
          m.put("phone", p.getPhone());
          m.put("email", p.getEmail());
          return m;
        })
        .toList();
  }

  public Map<String, Object> listOrders(String partnerId, int limit) {
    if (!StringUtils.hasText(partnerId)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "partner_id is required");
    }
    PartnerEntity partner = partnerRepository.findById(partnerId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partner not found"));

    int lim = Math.max(1, Math.min(limit <= 0 ? 40 : limit, 100));
    List<OrderEntity> orders = orderRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
        .limit(lim)
        .toList();

    List<String> ids = orders.stream().map(OrderEntity::getId).toList();
    Map<String, List<PartnerNotificationEntity>> byOrder = partnerNotifyService
        .listForPartnerOrders(partnerId, ids)
        .stream()
        .collect(Collectors.groupingBy(PartnerNotificationEntity::getOrderId));

    List<Map<String, Object>> rows = new ArrayList<>();
    for (OrderEntity o : orders) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", o.getId());
      row.put("status", o.getStatus());
      row.put("status_label", displayLabel(o));
      row.put("customer_confirmed", "delivered".equals(o.getStatus()) || "rated".equals(o.getStatus()));
      row.put("awaiting_customer_confirm", "delivering".equals(o.getStatus()));
      row.put("pickup_address", o.getPickupAddress());
      row.put("service_label", o.getServiceLabel());
      row.put("total", o.getTotal());
      row.put("currency", o.getCurrency());
      row.put("created_at", o.getCreatedAt());
      row.put("updated_at", o.getUpdatedAt());
      row.put("notifications", summarizeNotifications(byOrder.getOrDefault(o.getId(), List.of())));
      rows.add(row);
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("partner", Map.of(
        "id", partner.getId(),
        "name", partner.getName(),
        "city", partner.getCity(),
        "area", partner.getArea(),
        "phone", partner.getPhone() == null ? "" : partner.getPhone(),
        "email", partner.getEmail() == null ? "" : partner.getEmail()
    ));
    out.put("orders", rows);
    return out;
  }

  private static String displayLabel(OrderEntity o) {
    if ("delivered".equals(o.getStatus()) || "rated".equals(o.getStatus())) {
      return "Customer confirmed";
    }
    return o.getStatusLabel() != null ? o.getStatusLabel() : o.getStatus();
  }

  private static List<Map<String, Object>> summarizeNotifications(List<PartnerNotificationEntity> notes) {
    Map<String, PartnerNotificationEntity> latestByChannel = new LinkedHashMap<>();
    for (PartnerNotificationEntity n : notes) {
      latestByChannel.putIfAbsent(n.getChannel(), n);
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (PartnerNotificationEntity n : latestByChannel.values()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("channel", n.getChannel());
      m.put("status", n.getStatus());
      m.put("target", n.getTarget());
      m.put("deeplink", n.getDeeplink());
      m.put("created_at", n.getCreatedAt());
      out.add(m);
    }
    return out;
  }
}
