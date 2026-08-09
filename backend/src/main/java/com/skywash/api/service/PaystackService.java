package com.skywash.api.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.config.ApiException;
import com.skywash.api.config.PaystackProperties;

@Service
public class PaystackService {

  private final PaystackProperties props;
  private final ObjectMapper objectMapper;
  private final OrderService orderService;
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  public PaystackService(PaystackProperties props, ObjectMapper objectMapper, OrderService orderService) {
    this.props = props;
    this.objectMapper = objectMapper;
    this.orderService = orderService;
  }

  public Map<String, Object> initialize(String orderId, int amountNaira, String email) {
    if (orderId == null || orderId.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "order_id is required");
    }
    // Ensure order exists
    orderService.getEntity(orderId);

    if (amountNaira < 100) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "amount must be at least ₦100");
    }
    String reference = "SKY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    String safeEmail = (email == null || email.isBlank())
        ? "customer+" + orderId.replace("-", "").substring(0, Math.min(8, orderId.length())) + "@skywash.app"
        : email.trim();

    if (!props.isConfigured()) {
      orderService.attachPaymentReference(orderId, reference);
      Map<String, Object> stub = new LinkedHashMap<>();
      stub.put("ok", true);
      stub.put("provider", "paystack");
      stub.put("reference", reference);
      stub.put("order_id", orderId);
      stub.put("amount", amountNaira);
      stub.put("currency", "NGN");
      stub.put("public_key", props.getPublicKey());
      stub.put("authorization_url", "https://checkout.paystack.com/demo");
      stub.put("demo", true);
      stub.put("message", "Paystack secret key not configured — stub response");
      return stub;
    }

    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("email", safeEmail);
      payload.put("amount", amountNaira * 100); // Paystack expects kobo
      payload.put("currency", "NGN");
      payload.put("reference", reference);
      payload.put("callback_url", props.getCallbackUrl());
      payload.put("metadata", Map.of("order_id", orderId));

      JsonNode root = postJson("/transaction/initialize", payload);
      if (!root.path("status").asBoolean(false)) {
        throw new ApiException(HttpStatus.BAD_GATEWAY,
            root.path("message").asText("Paystack initialize failed"));
      }
      JsonNode data = root.path("data");
      String ref = data.path("reference").asText(reference);
      orderService.attachPaymentReference(orderId, ref);

      Map<String, Object> out = new LinkedHashMap<>();
      out.put("ok", true);
      out.put("provider", "paystack");
      out.put("reference", ref);
      out.put("access_code", data.path("access_code").asText(null));
      out.put("authorization_url", data.path("authorization_url").asText(null));
      out.put("order_id", orderId);
      out.put("amount", amountNaira);
      out.put("currency", "NGN");
      out.put("public_key", props.getPublicKey());
      out.put("demo", false);
      return out;
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Paystack error: " + ex.getMessage());
    }
  }

  public Map<String, Object> verify(String reference) {
    if (reference == null || reference.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "reference is required");
    }
    if (!props.isConfigured()) {
      return Map.of(
          "ok", true,
          "demo", true,
          "reference", reference,
          "status", "success",
          "message", "Paystack not configured — stub verify"
      );
    }
    try {
      JsonNode root = getJson("/transaction/verify/" + reference.trim());
      if (!root.path("status").asBoolean(false)) {
        throw new ApiException(HttpStatus.BAD_GATEWAY,
            root.path("message").asText("Paystack verify failed"));
      }
      JsonNode data = root.path("data");
      String status = data.path("status").asText();
      String channel = data.path("channel").asText(null);
      String orderId = data.path("metadata").path("order_id").asText(null);

      Map<String, Object> out = new LinkedHashMap<>();
      out.put("ok", true);
      out.put("demo", false);
      out.put("reference", data.path("reference").asText(reference));
      out.put("status", status);
      out.put("amount", data.path("amount").asInt(0) / 100);
      out.put("currency", data.path("currency").asText("NGN"));
      out.put("paid_at", data.path("paid_at").asText(null));
      out.put("channel", channel);
      if (orderId != null && !orderId.isBlank()) out.put("order_id", orderId);

      if ("success".equalsIgnoreCase(status)) {
        try {
          Map<String, Object> paid = orderService.markPaidByReference(
              data.path("reference").asText(reference), channel, orderId);
          out.put("order", paid);
          if (paid.get("id") != null) {
            out.put("order_id", String.valueOf(paid.get("id")));
          }
        } catch (ApiException ex) {
          out.put("order_update_error", ex.getMessage());
        }
      }
      return out;
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "Paystack verify error: " + ex.getMessage());
    }
  }

  /** Handle Paystack webhook. Verifies HMAC SHA512 signature when configured. */
  public Map<String, Object> handleWebhook(String rawBody, String signatureHeader) {
    if (props.isConfigured()) {
      if (signatureHeader == null || signatureHeader.isBlank() || !validSignature(rawBody, signatureHeader)) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Paystack signature");
      }
    }

    try {
      JsonNode root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
      String event = root.path("event").asText("");
      JsonNode data = root.path("data");
      String reference = data.path("reference").asText(null);
      String status = data.path("status").asText("");
      String channel = data.path("channel").asText(null);
      String orderId = data.path("metadata").path("order_id").asText(null);

      Map<String, Object> out = new LinkedHashMap<>();
      out.put("received", true);
      out.put("event", event);
      out.put("reference", reference);

      boolean successEvent = "charge.success".equals(event)
          || ("success".equalsIgnoreCase(status) && event.contains("charge"));
      if (successEvent && reference != null) {
        Map<String, Object> paid = orderService.markPaidByReference(reference, channel, orderId);
        out.put("handled", true);
        out.put("order", paid);
      } else {
        out.put("handled", false);
        out.put("message", "Event acknowledged without order update");
      }
      return out;
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid webhook payload: " + ex.getMessage());
    }
  }

  public boolean validSignature(String rawBody, String signatureHeader) {
    try {
      Mac mac = Mac.getInstance("HmacSHA512");
      mac.init(new SecretKeySpec(props.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
      String computed = HexFormat.of().formatHex(mac.doFinal((rawBody == null ? "" : rawBody).getBytes(StandardCharsets.UTF_8)));
      return computed.equalsIgnoreCase(signatureHeader.trim());
    } catch (Exception e) {
      return false;
    }
  }

  public Map<String, Object> publicConfig() {
    return Map.of(
        "provider", "paystack",
        "public_key", props.getPublicKey() == null ? "" : props.getPublicKey(),
        "configured", props.isConfigured()
    );
  }

  private JsonNode postJson(String path, Map<String, Object> body) throws Exception {
    String json = objectMapper.writeValueAsString(body);
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(props.getBaseUrl() + path))
        .timeout(Duration.ofSeconds(30))
        .header("Authorization", "Bearer " + props.getSecretKey())
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
    return objectMapper.readTree(res.body());
  }

  private JsonNode getJson(String path) throws Exception {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(props.getBaseUrl() + path))
        .timeout(Duration.ofSeconds(30))
        .header("Authorization", "Bearer " + props.getSecretKey())
        .GET()
        .build();
    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
    return objectMapper.readTree(res.body());
  }
}
