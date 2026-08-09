package com.skywash.api.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.config.ApiException;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private static final URI RESEND_URI = URI.create("https://api.resend.com/emails");

  private final JavaMailSender mailSender;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final boolean enabled;
  private final String from;
  private final String smtpUsername;
  private final String resendApiKey;

  public EmailService(
      JavaMailSender mailSender,
      ObjectMapper objectMapper,
      @Value("${skywash.mail.enabled:true}") boolean enabled,
      @Value("${skywash.mail.from:skyWash <onboarding@resend.dev>}") String from,
      @Value("${spring.mail.username:}") String smtpUsername,
      @Value("${resend.api-key:}") String resendApiKey
  ) {
    this.mailSender = mailSender;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    this.enabled = enabled;
    this.from = from;
    this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
    this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
  }

  public void sendOtp(String toEmail, String code) {
    String subject = "Your skyWash login code";
    String body = """
        Your skyWash verification code is:

        %s

        It expires in 10 minutes. If you didn’t request this, you can ignore this email.
        """.formatted(code);

    if (!enabled) {
      log.info("Mail disabled — OTP for {} is {}", toEmail, code);
      return;
    }

    if (StringUtils.hasText(resendApiKey)) {
      sendViaResend(toEmail, subject, body);
      return;
    }

    if (StringUtils.hasText(smtpUsername)) {
      sendViaSmtp(toEmail, subject, body);
      return;
    }

    throw new ApiException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Email login is not configured (set RESEND_API_KEY on the server)"
    );
  }

  private void sendViaResend(String toEmail, String subject, String body) {
    try {
      String json = objectMapper.writeValueAsString(new ResendPayload(from, new String[]{toEmail}, subject, body));
      HttpRequest request = HttpRequest.newBuilder(RESEND_URI)
          .timeout(Duration.ofSeconds(15))
          .header("Authorization", "Bearer " + resendApiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        log.info("OTP email sent via Resend to {}", toEmail);
        return;
      }
      log.error("Resend failed ({}): {}", response.statusCode(), response.body());
      throw new ApiException(
          HttpStatus.BAD_GATEWAY,
          "Could not send verification email via Resend (" + response.statusCode() + ")"
      );
    } catch (ApiException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Resend request error for {}: {}", toEmail, ex.getMessage());
      throw new ApiException(
          HttpStatus.BAD_GATEWAY,
          "Could not send verification email. Try Google sign-in or try again shortly."
      );
    }
  }

  private void sendViaSmtp(String toEmail, String subject, String body) {
    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setFrom(from);
      msg.setTo(toEmail);
      msg.setSubject(subject);
      msg.setText(body);
      mailSender.send(msg);
      log.info("OTP email sent via SMTP to {}", toEmail);
    } catch (MailException ex) {
      log.error("Failed to send OTP email to {}: {}", toEmail, ex.getMessage());
      throw new ApiException(
          HttpStatus.BAD_GATEWAY,
          "Could not send verification email. Check mail settings or try Google sign-in."
      );
    }
  }

  private record ResendPayload(String from, String[] to, String subject, String text) {}
}
