package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.config.ApiException;
import com.skywash.api.config.PaystackProperties;

@ExtendWith(MockitoExtension.class)
class PaystackServiceWebhookTest {

  @Mock private OrderService orderService;

  private PaystackProperties props;
  private PaystackService paystackService;

  @BeforeEach
  void setUp() {
    props = new PaystackProperties();
    props.setSecretKey("sk_test_webhook_secret");
    props.setPublicKey("pk_test_x");
    paystackService = new PaystackService(props, new ObjectMapper(), orderService);
  }

  @Test
  void webhookRejectsBadSignature() {
    assertThrows(ApiException.class,
        () -> paystackService.handleWebhook("{\"event\":\"charge.success\"}", "bad-sig"));
  }

  @Test
  void webhookMarksOrderPaidOnChargeSuccess() throws Exception {
    String body = """
        {"event":"charge.success","data":{"reference":"SKY_abc","status":"success","channel":"card","metadata":{"order_id":"o1"}}}
        """;
    String sig = hmac(body, props.getSecretKey());
    when(orderService.markPaidByReference(eq("SKY_abc"), eq("card"), eq("o1")))
        .thenReturn(Map.of("id", "o1", "payment_status", "paid"));

    Map<String, Object> res = paystackService.handleWebhook(body, sig);
    assertTrue((Boolean) res.get("handled"));
    assertEquals("charge.success", res.get("event"));
    verify(orderService).markPaidByReference("SKY_abc", "card", "o1");
  }

  @Test
  void webhookIgnoresNonSuccessEvents() throws Exception {
    String body = "{\"event\":\"transfer.success\",\"data\":{\"reference\":\"x\"}}";
    String sig = hmac(body, props.getSecretKey());
    Map<String, Object> res = paystackService.handleWebhook(body, sig);
    assertFalse((Boolean) res.get("handled"));
  }

  @Test
  void initializeRequiresExistingOrder() {
    when(orderService.getEntity("missing")).thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));
    assertThrows(ApiException.class, () -> paystackService.initialize("missing", 1000, "a@b.com"));
  }

  private static String hmac(String body, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA512");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
    return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
  }
}
