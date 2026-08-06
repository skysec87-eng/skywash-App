package com.skywash.api.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_sessions", indexes = {
    @Index(name = "idx_auth_sessions_user", columnList = "user_id")
})
public class AuthSessionEntity {

  @Id
  private String token;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
