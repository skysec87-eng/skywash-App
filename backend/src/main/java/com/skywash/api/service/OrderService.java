package com.skywash.api.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skywash.api.config.ApiException;
import com.skywash.api.data.SeedData;
import com.skywash.api.entity.OrderEntity;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.model.Order;
import com.skywash.api.repo.OrderRepository;

@Service
public class OrderService {

  private final CatalogService catalogService;
  private final PricingService pricingService;
  private final OrderRepository orderRepository;

  public OrderService(CatalogService catalogService, PricingService pricingService, OrderRepository orderRepository) {
    this.catalogService = catalogService;
    this.pricingService = pricingService;
    this.orderRepository = orderRepository;
  }

  @Transactional
  public Order create(Map<String, Object> body, String userId) {
    if (userId == null || userId.isBlank()) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in first");
    }
    String partnerId = str(body.get("partner_id"));
    PartnerEntity partner = catalogService.requirePartner(partnerId);

    @SuppressWarnings("unchecked")
    Map<String, Object> pickupRaw = (Map<String, Object>) body.get("pickup");
    if (pickupRaw == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "pickup is required");
    }
    double lat = toDouble(pickupRaw.get("lat"));
    double lng = toDouble(pickupRaw.get("lng"));
    String address = str(pickupRaw.get("address"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> services = (List<Map<String, Object>>) body.get("services");
    String promo = str(body.get("promo_code"));
    Map<String, Object> quote = pricingService.quote(services, promo);

    String paymentMethod = str(body.get("payment_method"));
    if (paymentMethod.isEmpty()) paymentMethod = "card";

    Instant scheduledAt = null;
    Object sched = body.get("scheduled_at");
    if (sched != null && !String.valueOf(sched).isBlank() && !"null".equals(String.valueOf(sched))) {
      scheduledAt = Instant.parse(String.valueOf(sched));
    }

    int qty = services.stream().mapToInt(s -> toInt(s.get("qty"), 1)).max().orElse(1);
    Order.Pricing pricing = pricingService.toPricing(quote);

    OrderEntity order = new OrderEntity();
    order.setId(UUID.randomUUID().toString());
    order.setStatus("confirmed");
    order.setStatusLabel(labelFor("confirmed"));
    order.setPickupLat(lat);
    order.setPickupLng(lng);
    order.setPickupAddress(address);
    order.setScheduledAt(scheduledAt);
    order.setPartnerId(partner.getId());
    order.setUserId(userId);
    order.setProviderName(partner.getName());
    order.setServices(services.stream()
        .map(s -> new OrderEntity.ServiceLineEmbed(str(s.get("type")), toInt(s.get("qty"), 1)))
        .toList());
    order.setQuantity(qty);
    order.setPaymentMethod(paymentMethod);
    order.setPaymentDisplay(paymentDisplay(paymentMethod));
    order.setPromoCode(promo.isBlank() ? null : promo.toUpperCase(Locale.ROOT));
    order.setBaseFee(pricing.baseFee());
    order.setServiceCost(pricing.serviceCost());
    order.setPlatformFee(pricing.platformFee());
    order.setDiscount(pricing.discount());
    order.setTotal(pricing.total());
    order.setCurrency(pricing.currency());
    order.setServiceLabel(String.valueOf(quote.get("label")));
    order.setCreatedAt(Instant.now());
    order.setUpdatedAt(Instant.now());
    order.setCourierLat(partner.getLat());
    order.setCourierLng(partner.getLng());
    order.setPaymentStatus("unpaid");
    order.getTimeline().add(new OrderEntity.TimelineEmbed("confirmed", labelFor("confirmed"), Instant.now()));

    return toModel(orderRepository.save(order));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(int limit, String userId) {
    if (userId == null || userId.isBlank()) {
      return List.of();
    }
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .limit(Math.max(1, Math.min(limit, 200)))
        .map(this::toHistoryItem)
        .toList();
  }

  @Transactional(readOnly = true)
  public OrderEntity getEntity(String id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
  }

  @Transactional(readOnly = true)
  public Order get(String id) {
    return toModel(getEntity(id));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getDetail(String id) {
    OrderEntity o = getEntity(id);
    PartnerEntity partner = catalogService.findById(o.getPartnerId()).orElse(null);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", o.getId());
    body.put("status", o.getStatus());
    body.put("status_label", o.getStatusLabel());
    body.put("partner", partner == null
        ? Map.of("id", o.getPartnerId(), "name", o.getProviderName())
        : Map.of("id", partner.getId(), "name", partner.getName(), "rating", partner.getRating(),
            "lat", partner.getLat(), "lng", partner.getLng()));
    body.put("pickup", Map.of("lat", o.getPickupLat(), "lng", o.getPickupLng(), "address", o.getPickupAddress()));
    if (o.getCourierLat() != null) {
      body.put("partner_location", Map.of("lat", o.getCourierLat(), "lng", o.getCourierLng()));
    }
    body.put("eta_label", etaLabel(o));
    body.put("timeline", o.getTimeline().stream()
        .map(t -> Map.of("key", t.getKey(), "label", t.getLabel(), "at", t.getAt()))
        .toList());
    body.put("pricing", Map.of(
        "base_fee", o.getBaseFee(),
        "service_cost", o.getServiceCost(),
        "platform_fee", o.getPlatformFee(),
        "discount", o.getDiscount(),
        "total", o.getTotal(),
        "currency", o.getCurrency()
    ));
    body.put("service_label", o.getServiceLabel());
    body.put("payment", o.getPaymentDisplay());
    body.put("payment_status", o.getPaymentStatus() == null ? "unpaid" : o.getPaymentStatus());
    body.put("payment_reference", o.getPaymentReference());
    body.put("payment_channel", o.getPaymentChannel());
    body.put("paid_at", o.getPaidAt());
    body.put("rating", o.getRating());
    body.put("scheduled_at", o.getScheduledAt());
    body.put("created_at", o.getCreatedAt());
    body.put("updated_at", o.getUpdatedAt());
    return body;
  }

  @Transactional
  public void attachPaymentReference(String orderId, String reference) {
    OrderEntity o = getEntity(orderId);
    o.setPaymentReference(reference);
    o.setPaymentStatus("pending");
    o.setUpdatedAt(Instant.now());
    orderRepository.save(o);
  }

  @Transactional
  public Map<String, Object> markPaid(String orderId, String reference, String channel) {
    OrderEntity o = getEntity(orderId);
    if ("paid".equalsIgnoreCase(o.getPaymentStatus())) {
      return Map.of(
          "id", o.getId(),
          "payment_status", "paid",
          "payment_reference", o.getPaymentReference() == null ? reference : o.getPaymentReference(),
          "already_paid", true
      );
    }
    o.setPaymentStatus("paid");
    o.setPaymentReference(reference);
    o.setPaymentChannel(channel);
    o.setPaidAt(Instant.now());
    o.setUpdatedAt(Instant.now());
    o.getTimeline().add(new OrderEntity.TimelineEmbed("paid", "Payment received", Instant.now()));
    orderRepository.save(o);
    return Map.of(
        "id", o.getId(),
        "payment_status", "paid",
        "payment_reference", reference,
        "already_paid", false
    );
  }

  @Transactional
  public Map<String, Object> markPaidByReference(String reference, String channel, String orderIdHint) {
    OrderEntity o = orderRepository.findByPaymentReference(reference).orElse(null);
    if (o == null && orderIdHint != null && !orderIdHint.isBlank()) {
      o = orderRepository.findById(orderIdHint).orElse(null);
    }
    if (o == null) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Order not found for payment reference: " + reference);
    }
    return markPaid(o.getId(), reference, channel);
  }

  @Transactional
  public Order cancel(String id) {
    OrderEntity o = getEntity(id);
    if (List.of("delivered", "rated", "cancelled").contains(o.getStatus())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Order cannot be cancelled in status: " + o.getStatus());
    }
    o.setStatus("cancelled");
    o.setStatusLabel("Cancelled");
    o.setUpdatedAt(Instant.now());
    o.getTimeline().add(new OrderEntity.TimelineEmbed("cancelled", "Cancelled", Instant.now()));
    return toModel(orderRepository.save(o));
  }

  @Transactional
  public Order rate(String id, int rating) {
    if (rating < 1 || rating > 5) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "rating must be 1–5");
    }
    OrderEntity o = getEntity(id);
    o.setRating(rating);
    o.setStatus("rated");
    o.setStatusLabel("Rated");
    o.setUpdatedAt(Instant.now());
    o.getTimeline().add(new OrderEntity.TimelineEmbed("rated", "Rated", Instant.now()));
    return toModel(orderRepository.save(o));
  }

  @Transactional
  public Order advanceStatus(String id, String status) {
    OrderEntity o = getEntity(id);
    String key = status.toLowerCase(Locale.ROOT);
    String label = labelFor(key);
    if (label == null && !"cancelled".equals(key)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status: " + status);
    }
    o.setStatus(key);
    o.setStatusLabel(label != null ? label : "Cancelled");
    o.setUpdatedAt(Instant.now());
    o.getTimeline().add(new OrderEntity.TimelineEmbed(key, o.getStatusLabel(), Instant.now()));
    return toModel(orderRepository.save(o));
  }

  @Scheduled(fixedDelay = 2000)
  @Transactional
  public void progressActiveOrders() {
    Instant now = Instant.now();
    List<String> terminal = List.of("cancelled", "delivered", "rated");
    for (OrderEntity o : orderRepository.findByStatusNotIn(terminal)) {
      if (o.getScheduledAt() != null && o.getScheduledAt().isAfter(now)) continue;

      int idx = indexOfStatus(o.getStatus());
      if (idx < 0 || idx >= SeedData.STATUSES.size() - 1) continue;

      var current = SeedData.STATUSES.get(idx);
      Instant last = o.getTimeline().isEmpty() ? o.getCreatedAt() : o.getTimeline().get(o.getTimeline().size() - 1).getAt();
      if (last == null) continue;
      if (now.toEpochMilli() - last.toEpochMilli() < current.durationMs()) continue;

      var next = SeedData.STATUSES.get(idx + 1);
      o.setStatus(next.key());
      o.setStatusLabel(next.label());
      o.setUpdatedAt(now);
      o.getTimeline().add(new OrderEntity.TimelineEmbed(next.key(), next.label(), now));

      if (o.getCourierLat() != null) {
        if ("enroute".equals(next.key()) || "delivering".equals(next.key())) {
          double t = "delivering".equals(next.key()) ? 0.15 : 0.35;
          o.setCourierLat(o.getCourierLat() + (o.getPickupLat() - o.getCourierLat()) * t);
          o.setCourierLng(o.getCourierLng() + (o.getPickupLng() - o.getCourierLng()) * t);
        }
      }
      orderRepository.save(o);
    }
  }

  private Order toModel(OrderEntity o) {
    Order m = new Order();
    m.setId(o.getId());
    m.setStatus(o.getStatus());
    m.setStatusLabel(o.getStatusLabel());
    m.setPickup(new Order.Pickup(o.getPickupLat(), o.getPickupLng(), o.getPickupAddress()));
    m.setScheduledAt(o.getScheduledAt());
    m.setPartnerId(o.getPartnerId());
    m.setProviderName(o.getProviderName());
    m.setServices(o.getServices().stream()
        .map(s -> new Order.ServiceLine(s.getType(), s.getQty()))
        .toList());
    m.setQuantity(o.getQuantity());
    m.setPaymentMethod(o.getPaymentMethod());
    m.setPaymentDisplay(o.getPaymentDisplay());
    m.setPromoCode(o.getPromoCode());
    m.setPricing(new Order.Pricing(o.getBaseFee(), o.getServiceCost(), o.getPlatformFee(),
        o.getDiscount(), o.getTotal(), o.getCurrency()));
    m.setRating(o.getRating());
    m.setServiceLabel(o.getServiceLabel());
    m.setTimeline(o.getTimeline().stream()
        .map(t -> new Order.TimelineEvent(t.getKey(), t.getLabel(), t.getAt()))
        .toList());
    m.setCreatedAt(o.getCreatedAt());
    m.setUpdatedAt(o.getUpdatedAt());
    m.setCourierLat(o.getCourierLat());
    m.setCourierLng(o.getCourierLng());
    return m;
  }

  private Map<String, Object> toHistoryItem(OrderEntity o) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", o.getId());
    m.put("provider_name", o.getProviderName());
    m.put("service_label", o.getServiceLabel());
    m.put("total", o.getTotal());
    m.put("total_display", "₦" + String.format(Locale.US, "%,d", o.getTotal()));
    m.put("payment", o.getPaymentDisplay());
    m.put("rating", o.getRating());
    m.put("status", o.getStatus());
    m.put("created_at", o.getCreatedAt());
    return m;
  }

  private String etaLabel(OrderEntity o) {
    if (o.getScheduledAt() != null) return o.getScheduledAt().toString();
    return switch (o.getStatus()) {
      case "confirmed" -> "Preparing…";
      case "enroute", "delivering" -> "~10 min";
      case "pickedup" -> "At pickup";
      case "washing" -> "In progress";
      case "delivered", "rated" -> "Arrived";
      default -> "—";
    };
  }

  private static String labelFor(String key) {
    return SeedData.STATUSES.stream()
        .filter(s -> s.key().equals(key))
        .map(SeedData.StatusDef::label)
        .findFirst()
        .orElse(null);
  }

  private static int indexOfStatus(String key) {
    for (int i = 0; i < SeedData.STATUSES.size(); i++) {
      if (SeedData.STATUSES.get(i).key().equals(key)) return i;
    }
    return -1;
  }

  private static String paymentDisplay(String key) {
    return switch (key) {
      case "transfer" -> "Bank Transfer";
      case "cash" -> "Cash on Pickup";
      default -> "Debit / Credit Card";
    };
  }

  private static String str(Object v) {
    return v == null ? "" : String.valueOf(v).trim();
  }

  private static int toInt(Object v, int fallback) {
    if (v == null) return fallback;
    if (v instanceof Number n) return n.intValue();
    return Integer.parseInt(String.valueOf(v));
  }

  private static double toDouble(Object v) {
    if (v instanceof Number n) return n.doubleValue();
    return Double.parseDouble(String.valueOf(v));
  }
}
