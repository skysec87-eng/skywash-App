package com.skywash.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Order {
  private String id;
  private String status;
  private String statusLabel;
  private Pickup pickup;
  private Instant scheduledAt;
  private String partnerId;
  private String providerName;
  private List<ServiceLine> services = new ArrayList<>();
  private int quantity;
  private String paymentMethod;
  private String paymentDisplay;
  private String promoCode;
  private Pricing pricing;
  private Integer rating;
  private String serviceLabel;
  private List<TimelineEvent> timeline = new ArrayList<>();
  private Instant createdAt;
  private Instant updatedAt;
  private Double courierLat;
  private Double courierLng;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getStatusLabel() { return statusLabel; }
  public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
  public Pickup getPickup() { return pickup; }
  public void setPickup(Pickup pickup) { this.pickup = pickup; }
  public Instant getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
  public String getPartnerId() { return partnerId; }
  public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
  public String getProviderName() { return providerName; }
  public void setProviderName(String providerName) { this.providerName = providerName; }
  public List<ServiceLine> getServices() { return services; }
  public void setServices(List<ServiceLine> services) { this.services = services; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public String getPaymentDisplay() { return paymentDisplay; }
  public void setPaymentDisplay(String paymentDisplay) { this.paymentDisplay = paymentDisplay; }
  public String getPromoCode() { return promoCode; }
  public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
  public Pricing getPricing() { return pricing; }
  public void setPricing(Pricing pricing) { this.pricing = pricing; }
  public Integer getRating() { return rating; }
  public void setRating(Integer rating) { this.rating = rating; }
  public String getServiceLabel() { return serviceLabel; }
  public void setServiceLabel(String serviceLabel) { this.serviceLabel = serviceLabel; }
  public List<TimelineEvent> getTimeline() { return timeline; }
  public void setTimeline(List<TimelineEvent> timeline) { this.timeline = timeline; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public Double getCourierLat() { return courierLat; }
  public void setCourierLat(Double courierLat) { this.courierLat = courierLat; }
  public Double getCourierLng() { return courierLng; }
  public void setCourierLng(Double courierLng) { this.courierLng = courierLng; }

  public record Pickup(double lat, double lng, String address) {}
  public record ServiceLine(String type, int qty) {}
  public record Pricing(int baseFee, int serviceCost, int platformFee, int discount, int total, String currency) {}
  public record TimelineEvent(String key, String label, Instant at) {}
}
