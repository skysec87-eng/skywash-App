package com.skywash.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.skywash.api.service.AuthService;
import com.skywash.api.service.PaystackService;

@WebMvcTest(controllers = AuthPaymentController.class)
class AuthPaymentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private PaystackService paystackService;

  @Test
  void signup() throws Exception {
    when(authService.signup("Ada", "+2348011112222", "pass123"))
        .thenReturn(Map.of(
            "ok", true,
            "token", "sw_token",
            "user", Map.of("id", "u1", "name", "Ada", "phone", "+2348011112222")
        ));

    mockMvc.perform(post("/api/auth/password-registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ada\",\"phone\":\"+2348011112222\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.token").value("sw_token"));
  }

  @Test
  void login() throws Exception {
    when(authService.login("+2348011112222", "pass123"))
        .thenReturn(Map.of("ok", true, "token", "sw_token", "user", Map.of("name", "Ada")));

    mockMvc.perform(post("/api/auth/password-sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"+2348011112222\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  @Test
  void sendOtp() throws Exception {
    when(authService.sendOtp("signup", "ada@example.com", "Ada", "+2348011112222"))
        .thenReturn(Map.of("ok", true, "email", "ada@example.com", "purpose", "signup"));

    mockMvc.perform(post("/api/auth/verification-codes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"purpose\":\"signup\",\"email\":\"ada@example.com\",\"name\":\"Ada\",\"phone\":\"+2348011112222\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  @Test
  void verifyOtp() throws Exception {
    when(authService.verifyOtp("ada@example.com", "123456"))
        .thenReturn(Map.of("ok", true, "registration_required", false, "token", "sw_token", "user", Map.of("name", "Ada")));

    mockMvc.perform(post("/api/auth/verification-codes/confirmations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ada@example.com\",\"code\":\"123456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("sw_token"));
  }

  @Test
  void completeProfile() throws Exception {
    when(authService.completeProfile("ptok", "Ada", "+2348011112222"))
        .thenReturn(Map.of("ok", true, "token", "sw_token", "user", Map.of("name", "Ada")));

    mockMvc.perform(post("/api/auth/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"registration_token\":\"ptok\",\"name\":\"Ada\",\"phone\":\"+2348011112222\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("sw_token"));
  }

  @Test
  void logout() throws Exception {
    mockMvc.perform(delete("/api/auth/sessions/current").header("Authorization", "Bearer sw_token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
    verify(authService).logout("Bearer sw_token");
  }

  @Test
  void logoutAll() throws Exception {
    when(authService.logoutAll("Bearer sw_token")).thenReturn(Map.of("ok", true));
    mockMvc.perform(delete("/api/auth/sessions").header("Authorization", "Bearer sw_token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  @Test
  void meWithoutAuthReturnsGuest() throws Exception {
    when(authService.me(isNull())).thenReturn(Map.of("id", "anon", "name", "Guest"));
    mockMvc.perform(get("/api/account"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.name").value("Guest"));
  }

  @Test
  void patchMe() throws Exception {
    when(authService.patchMe(eq("Bearer sw_token"), anyMap()))
        .thenReturn(Map.of("id", "u1", "name", "Ada"));

    mockMvc.perform(patch("/api/account")
            .header("Authorization", "Bearer sw_token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ada\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.name").value("Ada"));
  }

  @Test
  void initializePayment() throws Exception {
    when(paystackService.initialize(anyString(), anyInt(), anyString()))
        .thenReturn(Map.of("ok", true, "provider", "paystack", "reference", "SKY_test"));

    mockMvc.perform(post("/api/payments/checkouts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"order_id\":\"o1\",\"amount\":1935}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.provider").value("paystack"));
  }

  @Test
  void verifyPayment() throws Exception {
    when(paystackService.verify("SKY_ref"))
        .thenReturn(Map.of("ok", true, "status", "success", "reference", "SKY_ref"));

    mockMvc.perform(post("/api/payments/verifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reference\":\"SKY_ref\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void paymentWebhook() throws Exception {
    when(paystackService.handleWebhook(anyString(), any()))
        .thenReturn(Map.of("received", true, "handled", true, "event", "charge.success"));

    mockMvc.perform(post("/api/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-paystack-signature", "sig")
            .content("{\"event\":\"charge.success\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.received").value(true));
  }

  @Test
  void courierLocation() throws Exception {
    mockMvc.perform(post("/api/couriers/c1/location")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"lat\":6.45,\"lng\":3.47}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.courier_id").value("c1"));
  }
}
