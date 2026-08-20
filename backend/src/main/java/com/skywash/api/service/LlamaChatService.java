package com.skywash.api.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skywash.api.model.Order;

/**
 * skyWash Assist powered by Meta Llama via an OpenAI-compatible chat API
 * (Groq, Together, Fireworks, or Meta Llama API).
 */
@Service
public class LlamaChatService {

  private static final Logger log = LoggerFactory.getLogger(LlamaChatService.class);
  private static final Pattern ETA_ASK = Pattern.compile(
      "\\b(how much longer|how long|eta|when (will|are|is|do)|what time)\\b",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern SUPPORT_ASK = Pattern.compile(
      "\\b(support|human|agent|help desk|customer ?service|speak (to|with)|talk (to|with)|email|contact)\\b",
      Pattern.CASE_INSENSITIVE
  );

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String apiKey;
  private final String baseUrl;
  private final String model;
  private final String supportEmail;

  public LlamaChatService(
      ObjectMapper objectMapper,
      @Value("${llama.api-key:}") String apiKey,
      @Value("${llama.base-url:https://api.groq.com/openai/v1}") String baseUrl,
      @Value("${llama.model:llama-3.3-70b-versatile}") String model,
      @Value("${support.email:isaac.arinze.dev@gmail.com}") String supportEmail
  ) {
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.baseUrl = trimTrailingSlash(baseUrl == null ? "" : baseUrl.trim());
    this.model = model == null || model.isBlank() ? "llama-3.3-70b-versatile" : model.trim();
    this.supportEmail = StringUtils.hasText(supportEmail) ? supportEmail.trim() : "isaac.arinze.dev@gmail.com";
  }

  public boolean isEnabled() {
    return StringUtils.hasText(apiKey) && StringUtils.hasText(baseUrl);
  }

  public String replyAsAssist(Order order, String customerMessage, List<Map<String, Object>> history) {
    // Prefer deterministic, status-aware replies so chat never drifts from the trip UI.
    String fallback = contextualFallback(order, customerMessage, supportEmail);
    if (!isEnabled()) {
      return fallback;
    }
    try {
      String ai = callLlama(order, customerMessage, history);
      if (!StringUtils.hasText(ai)) return fallback;
      if (contradictsStatus(ai, order.getStatus())) {
        log.info("Discarding Llama reply that contradicts status={}", order.getStatus());
        return fallback;
      }
      return ai;
    } catch (Exception ex) {
      log.warn("Llama assist failed: {}", ex.getMessage());
      return fallback;
    }
  }

  public String replyAsPartner(Order order, String customerMessage, List<Map<String, Object>> history) {
    return replyAsAssist(order, customerMessage, history);
  }

  private String callLlama(Order order, String customerMessage, List<Map<String, Object>> history) throws Exception {
    String status = dash(order.getStatus());
    String system = """
        You are skyWash Assist for an on-demand laundry trip in Nigeria.
        Current order status is "%s" (%s). You MUST stay consistent with that status.
        Never say the order is delivered/completed unless status is delivered or rated.
        Never say the partner is en route unless status is enroute or delivering.
        If the customer asks for a human, agent, or support, help briefly then tell them to email %s and include order ID %s.
        Reply in 1–2 short sentences. No markdown. Do not mention that you are an AI.
        """.formatted(status, dash(order.getStatusLabel()), supportEmail, dash(order.getId()));

    StringBuilder user = new StringBuilder();
    user.append("Partner: ").append(dash(order.getProviderName())).append("\n");
    if (order.getPickup() != null) {
      user.append("Address: ").append(dash(order.getPickup().address())).append("\n");
    }
    user.append("Service: ").append(dash(order.getServiceLabel())).append("\n");
    if (history != null && !history.isEmpty()) {
      user.append("Recent chat:\n");
      int from = Math.max(0, history.size() - 8);
      for (int i = from; i < history.size(); i++) {
        Map<String, Object> m = history.get(i);
        user.append("- ").append(m.get("sender")).append(": ").append(m.get("text")).append("\n");
      }
    }
    user.append("Customer just said: ").append(customerMessage);

    List<Map<String, String>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content", system));
    messages.add(Map.of("role", "user", "content", user.toString()));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("messages", messages);
    body.put("temperature", 0.4);
    body.put("max_tokens", 120);

    URI uri = URI.create(baseUrl + "/chat/completions");
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      log.warn("Llama HTTP {}: {}", response.statusCode(), truncate(response.body()));
      return null;
    }
    JsonNode content = objectMapper.readTree(response.body())
        .path("choices").path(0).path("message").path("content");
    String text = content.asText("").trim();
    if (!StringUtils.hasText(text)) return null;
    return text.length() > 280 ? text.substring(0, 277) + "..." : text;
  }

  static boolean contradictsStatus(String reply, String status) {
    if (reply == null || status == null) return false;
    String r = reply.toLowerCase(Locale.ROOT);
    String s = status.toLowerCase(Locale.ROOT);
    boolean claimsDelivered = r.contains("already marked delivered")
        || r.contains("already delivered")
        || r.contains("order is delivered")
        || r.contains("has been delivered")
        || (r.contains("all done") && r.contains("rate"));
    boolean claimsEnroute = r.contains("on the way") || r.contains("en route") || r.contains("heading to you");
    if (claimsDelivered && !(s.equals("delivered") || s.equals("rated"))) return true;
    if (claimsEnroute && !(s.equals("enroute") || s.equals("delivering"))) {
      if (r.contains("is on the way") || r.contains("is heading") || r.contains("currently")) return true;
    }
    return false;
  }

  static String contextualFallback(Order order, String customerMessage, String supportEmail) {
    String msg = customerMessage == null ? "" : customerMessage.toLowerCase(Locale.ROOT);
    String status = order.getStatus() == null ? "" : order.getStatus();
    String partner = dash(order.getProviderName());
    String email = StringUtils.hasText(supportEmail) ? supportEmail.trim() : "isaac.arinze.dev@gmail.com";

    if (SUPPORT_ASK.matcher(msg).find()
        || msg.contains("speak to someone")
        || msg.contains("real person")
        || msg.contains("need help")) {
      String orderId = dash(order.getId());
      return "I can keep helping on this trip. For a human, email " + email
          + " and include order " + orderId + ".";
    }
    if (msg.contains("security") || msg.contains("gate") || msg.contains("leave the bag") || msg.contains("leaving with")) {
      return statusReplyPrefix(status, partner)
          + "Noted — I'll make sure " + partner + " knows about access.";
    }
    if (msg.contains("bell") || msg.contains("ring")) {
      return statusReplyPrefix(status, partner)
          + "Got it — we'll ring the bell on arrival.";
    }
    if (msg.contains("care") || msg.contains("delicate")) {
      return statusReplyPrefix(status, partner)
          + "Thanks for the care note — delicates will be handled gently.";
    }
    if (msg.contains("cancel")) {
      return "You can cancel with Cancel order below while pickup is still pending.";
    }
    if (ETA_ASK.matcher(msg).find()) {
      return switch (status) {
        case "confirmed" -> "Still confirmed — " + partner + " is preparing pickup. ETA is on the banner above.";
        case "enroute" -> partner + " is heading to you now. Watch the map for live progress.";
        case "pickedup", "washing" -> "Your bag is at the laundromat. I'll update you when delivery starts.";
        case "delivering" -> "Your clean laundry is on the way back — almost there.";
        case "delivered", "rated" -> "This order is already delivered. Rate the trip when you're ready.";
        default -> "I'm tracking this order live — check the status banner for the latest ETA.";
      };
    }
    return switch (status) {
      case "confirmed" -> "You're confirmed with " + partner + ". I'll notify you when they head out.";
      case "enroute" -> "On it — " + partner + " is en route. Share gate codes here if useful.";
      case "pickedup" -> "Bag collected. Washing starts next.";
      case "washing" -> "Still washing. I'll ping you when it's out for delivery.";
      case "delivering" -> "Delivery is in progress — hang tight.";
      case "delivered", "rated" -> "All done — this order was delivered. Tap the stars to rate.";
      default -> "Noted. I'll keep you posted as the trip moves forward.";
    };
  }

  private static String statusReplyPrefix(String status, String partner) {
    return switch (status) {
      case "delivered", "rated" -> "Order is already delivered. ";
      case "washing", "pickedup" -> "Your laundry is with " + partner + " now. ";
      case "enroute", "delivering" -> partner + " is moving on the map. ";
      default -> "";
    };
  }

  private static String dash(String v) {
    return v == null || v.isBlank() ? "—" : v;
  }

  private static String truncate(String s) {
    if (s == null) return "";
    return s.length() > 300 ? s.substring(0, 300) + "…" : s;
  }

  private static String trimTrailingSlash(String url) {
    if (url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }
}
