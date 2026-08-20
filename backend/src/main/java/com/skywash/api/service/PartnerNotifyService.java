package com.skywash.api.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.entity.OrderEntity;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.PartnerNotificationEntity;
import com.skywash.api.repo.PartnerNotificationRepository;
import com.skywash.api.repo.PartnerRepository;

@Service
public class PartnerNotifyService {

  public static final String EVENT_DELIVERY_CONFIRMED = "delivery_confirmed";
  public static final String CHANNEL_EMAIL = "email";
  public static final String CHANNEL_WHATSAPP = "whatsapp";
  public static final String STATUS_SENT = "sent";
  public static final String STATUS_QUEUED = "queued";
  public static final String STATUS_FAILED = "failed";

  private static final Logger log = LoggerFactory.getLogger(PartnerNotifyService.class);

  private final PartnerRepository partnerRepository;
  private final PartnerNotificationRepository notificationRepository;
  private final EmailService emailService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String fallbackEmail;
  private final String whatsappToken;
  private final String whatsappPhoneNumberId;

  public PartnerNotifyService(
      PartnerRepository partnerRepository,
      PartnerNotificationRepository notificationRepository,
      EmailService emailService,
      ObjectMapper objectMapper,
      @Value("${partner.notify.fallback-email:}") String fallbackEmail,
      @Value("${whatsapp.token:}") String whatsappToken,
      @Value("${whatsapp.phone-number-id:}") String whatsappPhoneNumberId
  ) {
    this.partnerRepository = partnerRepository;
    this.notificationRepository = notificationRepository;
    this.emailService = emailService;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    this.fallbackEmail = fallbackEmail == null ? "" : fallbackEmail.trim();
    this.whatsappToken = whatsappToken == null ? "" : whatsappToken.trim();
    this.whatsappPhoneNumberId = whatsappPhoneNumberId == null ? "" : whatsappPhoneNumberId.trim();
  }

  /** Best-effort partner notify after customer confirms receipt. Never throws. */
  public void onDeliveryConfirmed(OrderEntity order) {
    if (order == null) return;
    try {
      PartnerEntity partner = partnerRepository.findById(order.getPartnerId()).orElse(null);
      String message = buildMessage(order, partner);
      notifyEmail(order, partner, message);
      notifyWhatsApp(order, partner, message);
    } catch (Exception ex) {
      log.error("Partner notify failed for order {}: {}", order.getId(), ex.getMessage());
    }
  }

  public List<PartnerNotificationEntity> listForOrder(String orderId) {
    return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
  }

  public List<PartnerNotificationEntity> listForPartnerOrders(String partnerId, List<String> orderIds) {
    if (orderIds == null || orderIds.isEmpty()) return List.of();
    return notificationRepository.findByPartnerIdAndOrderIdIn(partnerId, orderIds);
  }

  private void notifyEmail(OrderEntity order, PartnerEntity partner, String message) {
    String to = resolveEmail(partner);
    PartnerNotificationEntity n = baseNotification(order, CHANNEL_EMAIL);
    n.setTarget(to);
    n.setPayload(message);
    if (!StringUtils.hasText(to)) {
      n.setStatus(STATUS_FAILED);
      n.setPayload("No partner email configured");
      notificationRepository.save(n);
      return;
    }
    String subject = "skyWash — customer confirmed receipt · order " + shortId(order.getId());
    boolean ok = emailService.sendPartnerAlert(to, subject, message);
    n.setStatus(ok ? STATUS_SENT : STATUS_FAILED);
    notificationRepository.save(n);
  }

  private void notifyWhatsApp(OrderEntity order, PartnerEntity partner, String message) {
    String phone = partner == null ? null : partner.getPhone();
    String e164 = toE164Digits(phone);
    String deeplink = null;
    if (StringUtils.hasText(e164)) {
      deeplink = "https://wa.me/" + e164 + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    PartnerNotificationEntity n = baseNotification(order, CHANNEL_WHATSAPP);
    n.setTarget(StringUtils.hasText(e164) ? "+" + e164 : phone);
    n.setPayload(message);
    n.setDeeplink(deeplink);

    if (!StringUtils.hasText(e164)) {
      n.setStatus(STATUS_FAILED);
      n.setPayload("No partner phone configured");
      notificationRepository.save(n);
      return;
    }

    if (StringUtils.hasText(whatsappToken) && StringUtils.hasText(whatsappPhoneNumberId)) {
      try {
        sendWhatsAppCloud(e164, message);
        n.setStatus(STATUS_SENT);
        notificationRepository.save(n);
        return;
      } catch (Exception ex) {
        log.warn("WhatsApp Cloud API failed for order {}: {}", order.getId(), ex.getMessage());
        n.setStatus(STATUS_QUEUED);
        notificationRepository.save(n);
        return;
      }
    }

    n.setStatus(STATUS_QUEUED);
    notificationRepository.save(n);
  }

  private void sendWhatsAppCloud(String e164Digits, String message) throws Exception {
    URI uri = URI.create(
        "https://graph.facebook.com/v19.0/" + whatsappPhoneNumberId + "/messages"
    );
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("messaging_product", "whatsapp");
    body.put("to", e164Digits);
    body.put("type", "text");
    body.put("text", Map.of("preview_url", false, "body", message));

    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(15))
        .header("Authorization", "Bearer " + whatsappToken)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("HTTP " + response.statusCode() + ": " + truncate(response.body()));
    }
  }

  private String resolveEmail(PartnerEntity partner) {
    if (partner != null && StringUtils.hasText(partner.getEmail())) {
      return partner.getEmail().trim();
    }
    if (StringUtils.hasText(fallbackEmail)) {
      return fallbackEmail.trim();
    }
    return null;
  }

  private PartnerNotificationEntity baseNotification(OrderEntity order, String channel) {
    PartnerNotificationEntity n = new PartnerNotificationEntity();
    n.setId(UUID.randomUUID().toString());
    n.setOrderId(order.getId());
    n.setPartnerId(order.getPartnerId());
    n.setEvent(EVENT_DELIVERY_CONFIRMED);
    n.setChannel(channel);
    n.setCreatedAt(Instant.now());
    return n;
  }

  static String buildMessage(OrderEntity order, PartnerEntity partner) {
    String partnerName = partner != null ? partner.getName() : order.getProviderName();
    return """
        skyWash partner alert

        Customer confirmed receipt — no issues reported.

        Partner: %s
        Order: %s
        Status: Customer confirmed
        Service: %s
        Address: %s
        Total: %s %s

        Please close this trip on your side. Thank you.
        """.formatted(
        dash(partnerName),
        dash(order.getId()),
        dash(order.getServiceLabel()),
        dash(order.getPickupAddress()),
        order.getCurrency() == null ? "NGN" : order.getCurrency(),
        order.getTotal()
    ).trim();
  }

  /** Digits only, Nigeria-friendly (+234… / 0…). */
  static String toE164Digits(String phone) {
    if (phone == null || phone.isBlank()) return null;
    String digits = phone.replaceAll("\\D+", "");
    if (digits.isEmpty()) return null;
    if (digits.startsWith("0") && digits.length() == 11) {
      digits = "234" + digits.substring(1);
    }
    if (digits.length() < 10) return null;
    return digits;
  }

  private static String shortId(String id) {
    if (id == null) return "—";
    return id.length() <= 8 ? id : id.substring(0, 8);
  }

  private static String dash(String v) {
    return v == null || v.isBlank() ? "—" : v;
  }

  private static String truncate(String s) {
    if (s == null) return "";
    return s.length() > 240 ? s.substring(0, 240) + "…" : s;
  }
}
