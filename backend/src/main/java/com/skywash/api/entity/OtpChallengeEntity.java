package com.skywash.api.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "otp_challenges", indexes = {
    @Index(name = "idx_otp_email", columnList = "email")
})
public class OtpChallengeEntity {

  @Id
  private String id;

  @Column(nullable = false)
  private String email;

  @Column(name = "code_hash", nullable = false)
  private String codeHash;

  /** signup | login */
  @Column(nullable = false, length = 16)
  private String purpose;

  @Column(name = "pending_name")
  private String pendingName;

  @Column(name = "pending_phone")
  private String pendingPhone;

  @Column(name = "pending_google_sub")
  private String pendingGoogleSub;

  @Column(nullable = false)
  private int attempts = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean consumed = false;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getCodeHash() { return codeHash; }
  public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
  public String getPurpose() { return purpose; }
  public void setPurpose(String purpose) { this.purpose = purpose; }
  public String getPendingName() { return pendingName; }
  public void setPendingName(String pendingName) { this.pendingName = pendingName; }
  public String getPendingPhone() { return pendingPhone; }
  public void setPendingPhone(String pendingPhone) { this.pendingPhone = pendingPhone; }
  public String getPendingGoogleSub() { return pendingGoogleSub; }
  public void setPendingGoogleSub(String pendingGoogleSub) { this.pendingGoogleSub = pendingGoogleSub; }
  public int getAttempts() { return attempts; }
  public void setAttempts(int attempts) { this.attempts = attempts; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public boolean isConsumed() { return consumed; }
  public void setConsumed(boolean consumed) { this.consumed = consumed; }
  public boolean isEmailVerified() { return emailVerified; }
  public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
}
