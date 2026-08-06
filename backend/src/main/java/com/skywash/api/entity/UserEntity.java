package com.skywash.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String phone;

  @Column(unique = true)
  private String email;

  @Column(name = "google_sub", unique = true)
  private String googleSub;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "payment_key")
  private String paymentKey = "card";

  @Column(name = "payment_name")
  private String paymentName = "Debit / Credit Card";

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getGoogleSub() { return googleSub; }
  public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getPaymentKey() { return paymentKey; }
  public void setPaymentKey(String paymentKey) { this.paymentKey = paymentKey; }
  public String getPaymentName() { return paymentName; }
  public void setPaymentName(String paymentName) { this.paymentName = paymentName; }
}
