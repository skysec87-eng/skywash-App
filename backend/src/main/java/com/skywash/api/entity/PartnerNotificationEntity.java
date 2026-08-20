package com.skywash.api.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "partner_notifications",
    indexes = {
        @Index(name = "idx_partner_notif_order", columnList = "order_id"),
        @Index(name = "idx_partner_notif_partner", columnList = "partner_id")
    }
)
public class PartnerNotificationEntity {

  @Id
  private String id;

  @Column(name = "order_id", nullable = false)
  private String orderId;

  @Column(name = "partner_id", nullable = false)
  private String partnerId;

  @Column(nullable = false, length = 64)
  private String event;

  @Column(nullable = false, length = 32)
  private String channel; // email | whatsapp

  @Column(nullable = false, length = 32)
  private String status; // sent | queued | failed

  @Column(length = 256)
  private String target;

  @Column(length = 2000)
  private String payload;

  @Column(length = 1000)
  private String deeplink;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getOrderId() { return orderId; }
  public void setOrderId(String orderId) { this.orderId = orderId; }
  public String getPartnerId() { return partnerId; }
  public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
  public String getEvent() { return event; }
  public void setEvent(String event) { this.event = event; }
  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getTarget() { return target; }
  public void setTarget(String target) { this.target = target; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public String getDeeplink() { return deeplink; }
  public void setDeeplink(String deeplink) { this.deeplink = deeplink; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
