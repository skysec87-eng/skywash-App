package com.skywash.api.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id
  private String id;

  @Column(nullable = false)
  private String status;

  @Column(name = "status_label", nullable = false)
  private String statusLabel;

  @Column(name = "pickup_lat", nullable = false)
  private double pickupLat;

  @Column(name = "pickup_lng", nullable = false)
  private double pickupLng;

  @Column(name = "pickup_address")
  private String pickupAddress;

  @Column(name = "scheduled_at")
  private Instant scheduledAt;

  @Column(name = "partner_id", nullable = false)
  private String partnerId;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "provider_name", nullable = false)
  private String providerName;

  @ElementCollection
  @CollectionTable(name = "order_services", joinColumns = @JoinColumn(name = "order_id"))
  @OrderColumn(name = "idx")
  private List<ServiceLineEmbed> services = new ArrayList<>();

  private int quantity;

  @Column(name = "payment_method")
  private String paymentMethod;

  @Column(name = "payment_display")
  private String paymentDisplay;

  @Column(name = "promo_code")
  private String promoCode;

  @Column(name = "base_fee")
  private int baseFee;

  @Column(name = "service_cost")
  private int serviceCost;

  @Column(name = "platform_fee")
  private int platformFee;

  private int discount;
  private int total;

  @Column(nullable = false)
  private String currency = "NGN";

  private Integer rating;

  @Column(name = "service_label", length = 1024)
  private String serviceLabel;

  @ElementCollection
  @CollectionTable(name = "order_timeline", joinColumns = @JoinColumn(name = "order_id"))
  @OrderColumn(name = "idx")
  private List<TimelineEmbed> timeline = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "courier_lat")
  private Double courierLat;

  @Column(name = "courier_lng")
  private Double courierLng;

  @Column(name = "payment_status")
  private String paymentStatus = "unpaid";

  @Column(name = "payment_reference")
  private String paymentReference;

  @Column(name = "payment_channel")
  private String paymentChannel;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Embeddable
  public static class ServiceLineEmbed {
    private String type;
    private int qty;
    public ServiceLineEmbed() {}
    public ServiceLineEmbed(String type, int qty) { this.type = type; this.qty = qty; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
  }

  @Embeddable
  public static class TimelineEmbed {
    private String key;
    private String label;
    private Instant at;
    public TimelineEmbed() {}
    public TimelineEmbed(String key, String label, Instant at) {
      this.key = key; this.label = label; this.at = at;
    }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getStatusLabel() { return statusLabel; }
  public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
  public double getPickupLat() { return pickupLat; }
  public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }
  public double getPickupLng() { return pickupLng; }
  public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }
  public String getPickupAddress() { return pickupAddress; }
  public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
  public Instant getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
  public String getPartnerId() { return partnerId; }
  public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getProviderName() { return providerName; }
  public void setProviderName(String providerName) { this.providerName = providerName; }
  public List<ServiceLineEmbed> getServices() { return services; }
  public void setServices(List<ServiceLineEmbed> services) { this.services = services; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
  public String getPaymentDisplay() { return paymentDisplay; }
  public void setPaymentDisplay(String paymentDisplay) { this.paymentDisplay = paymentDisplay; }
  public String getPromoCode() { return promoCode; }
  public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
  public int getBaseFee() { return baseFee; }
  public void setBaseFee(int baseFee) { this.baseFee = baseFee; }
  public int getServiceCost() { return serviceCost; }
  public void setServiceCost(int serviceCost) { this.serviceCost = serviceCost; }
  public int getPlatformFee() { return platformFee; }
  public void setPlatformFee(int platformFee) { this.platformFee = platformFee; }
  public int getDiscount() { return discount; }
  public void setDiscount(int discount) { this.discount = discount; }
  public int getTotal() { return total; }
  public void setTotal(int total) { this.total = total; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public Integer getRating() { return rating; }
  public void setRating(Integer rating) { this.rating = rating; }
  public String getServiceLabel() { return serviceLabel; }
  public void setServiceLabel(String serviceLabel) { this.serviceLabel = serviceLabel; }
  public List<TimelineEmbed> getTimeline() { return timeline; }
  public void setTimeline(List<TimelineEmbed> timeline) { this.timeline = timeline; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public Double getCourierLat() { return courierLat; }
  public void setCourierLat(Double courierLat) { this.courierLat = courierLat; }
  public Double getCourierLng() { return courierLng; }
  public void setCourierLng(Double courierLng) { this.courierLng = courierLng; }
  public String getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
  public String getPaymentReference() { return paymentReference; }
  public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
  public String getPaymentChannel() { return paymentChannel; }
  public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }
  public Instant getPaidAt() { return paidAt; }
  public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
