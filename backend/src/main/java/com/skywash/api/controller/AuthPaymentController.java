package com.skywash.api.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skywash.api.service.AuthService;
import com.skywash.api.service.PaystackService;

@RestController
@RequestMapping("/api")
public class AuthPaymentController {

  private final AuthService authService;
  private final PaystackService paystackService;
  private final String frontendCallbackBase;

  public AuthPaymentController(
      AuthService authService,
      PaystackService paystackService,
      @Value("${paystack.frontend-callback-url:http://127.0.0.1:3000/?payment=callback}") String frontendCallbackBase
  ) {
    this.authService = authService;
    this.paystackService = paystackService;
    this.frontendCallbackBase = frontendCallbackBase;
  }

  /** Legacy password registration (tests / admin tooling). Prefer OTP registration. */
  @PostMapping("/auth/password-registrations")
  public Map<String, Object> passwordRegistration(@RequestBody Map<String, Object> body) {
    return authService.signup(
        str(body.get("name")),
        str(body.get("phone")),
        str(body.get("password"))
    );
  }

  /** Legacy password login. Prefer OTP sessions. */
  @PostMapping("/auth/password-sessions")
  public Map<String, Object> passwordSession(@RequestBody Map<String, Object> body) {
    return authService.login(str(body.get("phone")), str(body.get("password")));
  }

  /** Request a one-time email verification code. */
  @PostMapping("/auth/verification-codes")
  public Map<String, Object> createVerificationCode(@RequestBody Map<String, Object> body) {
    return authService.sendOtp(
        str(body.get("purpose")),
        str(body.get("email")),
        str(body.get("name")),
        str(body.get("phone"))
    );
  }

  /** Confirm a verification code — returns a session, or a registration token for new accounts. */
  @PostMapping("/auth/verification-codes/confirmations")
  public Map<String, Object> confirmVerificationCode(@RequestBody Map<String, Object> body) {
    return authService.verifyOtp(str(body.get("email")), str(body.get("code")));
  }

  /** Finish OTP registration with name + phone after email confirmation. */
  @PostMapping("/auth/registrations")
  public Map<String, Object> completeRegistration(@RequestBody Map<String, Object> body) {
    String registrationToken = firstNonBlank(body.get("registration_token"), body.get("profile_token"));
    return authService.completeProfile(
        registrationToken,
        str(body.get("name")),
        str(body.get("phone"))
    );
  }

  @GetMapping("/auth/google/config")
  public Map<String, Object> googleConfig() {
    return authService.googleConfig();
  }

  /** Exchange a Google ID token for a skyWash session (or registration token). */
  @PostMapping("/auth/google")
  public Map<String, Object> googleSignIn(@RequestBody Map<String, Object> body) {
    return authService.loginWithGoogle(firstNonBlank(body.get("id_token"), body.get("credential")));
  }

  /** End the current session. */
  @DeleteMapping("/auth/sessions/current")
  public Map<String, Object> revokeCurrentSession(
      @RequestHeader(value = "Authorization", required = false) String auth
  ) {
    authService.logout(auth);
    return Map.of("ok", true);
  }

  /** End every session for the authenticated account. */
  @DeleteMapping("/auth/sessions")
  public Map<String, Object> revokeAllSessions(
      @RequestHeader(value = "Authorization", required = false) String auth
  ) {
    return authService.logoutAll(auth);
  }

  @GetMapping("/account")
  public Map<String, Object> getAccount(@RequestHeader(value = "Authorization", required = false) String auth) {
    return Map.of("user", authService.me(auth));
  }

  @PatchMapping("/account")
  public Map<String, Object> updateAccount(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Map<String, Object> body
  ) {
    return Map.of("user", authService.patchMe(auth, body));
  }

  @GetMapping("/payments/config")
  public Map<String, Object> paymentConfig() {
    return paystackService.publicConfig();
  }

  @PostMapping("/payments/checkouts")
  public Map<String, Object> createCheckout(@RequestBody Map<String, Object> body) {
    String orderId = str(body.get("order_id"));
    String email = str(body.get("email"));
    int amount = body.get("amount") instanceof Number n ? n.intValue() : 0;
    return paystackService.initialize(orderId, amount, email);
  }

  @PostMapping("/payments/verifications")
  public Map<String, Object> verifyPayment(@RequestBody Map<String, Object> body) {
    return paystackService.verify(str(body.get("reference")));
  }

  /**
   * HTTPS landing page for Paystack (dashboard requires https).
   * Redirects into the local frontend with the payment reference.
   */
  @GetMapping("/payments/callback")
  public ResponseEntity<Void> paymentCallback(
      @RequestParam(value = "reference", required = false) String reference,
      @RequestParam(value = "trxref", required = false) String trxref
  ) {
    String ref = (reference != null && !reference.isBlank()) ? reference : trxref;
    String target = frontendCallbackBase;
    if (ref != null && !ref.isBlank()) {
      String sep = target.contains("?") ? "&" : "?";
      target = target + sep + "reference=" + URLEncoder.encode(ref, StandardCharsets.UTF_8);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(URI.create(target));
    return new ResponseEntity<>(headers, HttpStatus.FOUND);
  }

  @PostMapping("/payments/webhook")
  public Map<String, Object> webhook(
      @RequestBody String rawBody,
      @RequestHeader(value = "x-paystack-signature", required = false) String signature
  ) {
    return paystackService.handleWebhook(rawBody, signature);
  }

  @PostMapping("/couriers/{id}/location")
  public Map<String, Object> courierLocation(
      @PathVariable String id,
      @RequestBody Map<String, Object> body
  ) {
    return Map.of(
        "ok", true,
        "courier_id", id,
        "lat", body.get("lat"),
        "lng", body.get("lng"),
        "demo", true
    );
  }

  private static String str(Object v) {
    return v == null ? "" : String.valueOf(v).trim();
  }

  private static String firstNonBlank(Object a, Object b) {
    String sa = str(a);
    return sa.isEmpty() ? str(b) : sa;
  }
}
