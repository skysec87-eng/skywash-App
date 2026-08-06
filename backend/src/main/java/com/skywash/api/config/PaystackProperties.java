package com.skywash.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paystack")
public class PaystackProperties {
  private String secretKey = "";
  private String publicKey = "";
  private String callbackUrl = "http://127.0.0.1:3000/?payment=callback";
  private String baseUrl = "https://api.paystack.co";

  public String getSecretKey() { return secretKey; }
  public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
  public String getPublicKey() { return publicKey; }
  public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
  public String getCallbackUrl() { return callbackUrl; }
  public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

  public boolean isConfigured() {
    return secretKey != null && !secretKey.isBlank();
  }
}
