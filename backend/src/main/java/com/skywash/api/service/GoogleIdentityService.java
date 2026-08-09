package com.skywash.api.service;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.skywash.api.config.ApiException;

@Service
public class GoogleIdentityService {

  private static final Logger log = LoggerFactory.getLogger(GoogleIdentityService.class);

  public record GoogleProfile(String subject, String email, String name) {}

  private final String clientId;
  private final GoogleIdTokenVerifier verifier;

  public GoogleIdentityService(@Value("${google.oauth.client-id:}") String clientId) {
    String resolved = firstNonBlank(
        clientId,
        System.getenv("GOOGLE_OAUTH_CLIENT_ID"),
        System.getenv("google.oauth.client-id")
    );
    this.clientId = stripQuotes(resolved);
    if (this.clientId.isEmpty()) {
      this.verifier = null;
      log.warn("Google OAuth is NOT configured (GOOGLE_OAUTH_CLIENT_ID missing/empty)");
    } else {
      this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
          .setAudience(Collections.singletonList(this.clientId))
          .build();
      log.info("Google OAuth configured (client_id ends with ...{})",
          this.clientId.substring(Math.max(0, this.clientId.length() - 12)));
    }
  }

  private static String firstNonBlank(String... values) {
    if (values == null) return "";
    for (String v : values) {
      if (v != null && !v.isBlank()) return v.trim();
    }
    return "";
  }

  private static String stripQuotes(String value) {
    if (value == null) return "";
    String resolved = value.trim();
    if (resolved.length() >= 2
        && ((resolved.startsWith("\"") && resolved.endsWith("\""))
            || (resolved.startsWith("'") && resolved.endsWith("'")))) {
      return resolved.substring(1, resolved.length() - 1).trim();
    }
    return resolved;
  }

  public boolean isConfigured() {
    return verifier != null;
  }

  public String clientId() {
    return clientId;
  }

  public GoogleProfile verifyIdToken(String idToken) {
    if (!isConfigured()) {
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured");
    }
    if (idToken == null || idToken.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "id_token is required");
    }
    try {
      GoogleIdToken token = verifier.verify(idToken);
      if (token == null) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
      }
      GoogleIdToken.Payload payload = token.getPayload();
      String email = payload.getEmail();
      if (email == null || email.isBlank()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Google account has no email");
      }
      if (Boolean.FALSE.equals(payload.getEmailVerified())) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
      }
      String name = payload.get("name") != null ? String.valueOf(payload.get("name")) : email.split("@")[0];
      return new GoogleProfile(payload.getSubject(), email.toLowerCase().trim(), name.trim());
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Could not verify Google token");
    }
  }
}
