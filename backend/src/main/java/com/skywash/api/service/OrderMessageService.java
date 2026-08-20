package com.skywash.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import com.skywash.api.entity.OrderEntity;
import com.skywash.api.model.Order;

/**
 * In-memory trip chat: customer ↔ skyWash Assist, plus proactive status updates.
 */
@Service
public class OrderMessageService {

  private final ConcurrentHashMap<String, List<Map<String, Object>>> threads = new ConcurrentHashMap<>();
  private final LlamaChatService llamaChatService;

  public OrderMessageService(LlamaChatService llamaChatService) {
    this.llamaChatService = llamaChatService;
  }

  public List<Map<String, Object>> list(String orderId) {
    return List.copyOf(threads.getOrDefault(orderId, List.of()));
  }

  public List<Map<String, Object>> sendCustomerMessage(Order order, String text) {
    List<Map<String, Object>> thread = threadFor(order.getId());
    thread.add(message("customer", text));
    String reply = llamaChatService.replyAsAssist(order, text, thread);
    thread.add(message("assist", reply));
    return List.copyOf(thread);
  }

  /** Clear chat when a trip ends so a new booking starts fresh. */
  public void clear(String orderId) {
    if (orderId != null) threads.remove(orderId);
  }

  public void announceStatus(OrderEntity order, String statusKey) {
    String text = announcementFor(order, statusKey);
    if (text == null) return;
    threadFor(order.getId()).add(message("system", text));
  }

  public void seedTripStart(OrderEntity order) {
    threadFor(order.getId()).add(message(
        "system",
        "You're booked with " + safe(order.getProviderName())
            + ". I'll keep you updated as your pickup progresses — ask me anything about timing or access. "
            + "Need a human? Email isaac.arinze.dev@gmail.com with this order ID."
    ));
  }

  private List<Map<String, Object>> threadFor(String orderId) {
    return threads.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>());
  }

  private static Map<String, Object> message(String sender, String text) {
    Map<String, Object> msg = new LinkedHashMap<>();
    msg.put("id", UUID.randomUUID().toString());
    msg.put("sender", sender);
    msg.put("text", text);
    msg.put("created_at", Instant.now().toString());
    return msg;
  }

  private static String announcementFor(OrderEntity order, String statusKey) {
    String partner = safe(order.getProviderName());
    return switch (statusKey) {
      case "enroute" -> partner + " is on the way to pick up your laundry. Share gate/security notes here if needed.";
      case "pickedup" -> "Bag collected. Heading to the laundromat now.";
      case "washing" -> "Your laundry is being washed and folded. I'll ping you when it's out for delivery.";
      case "delivering" -> "Fresh laundry is on the way back to you.";
      case "awaiting_confirmation" -> "Your laundry has arrived. Tap Confirm receipt when you’ve received it without issues.";
      case "delivered" -> "Delivery confirmed — enjoy the fresh clothes! Rate your experience when you're ready.";
      default -> null;
    };
  }

  private static String safe(String v) {
    return v == null || v.isBlank() ? "your partner" : v;
  }

  /** Test helper — clear threads between tests if needed. */
  void clearAll() {
    threads.clear();
  }

  List<Map<String, Object>> snapshot(String orderId) {
    return new ArrayList<>(threads.getOrDefault(orderId, List.of()));
  }
}
