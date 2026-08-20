package com.skywash.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skywash.api.config.ApiException;
import com.skywash.api.entity.UserEntity;
import com.skywash.api.model.Order;
import com.skywash.api.service.AuthService;
import com.skywash.api.service.OrderMessageService;
import com.skywash.api.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;
  private final AuthService authService;
  private final OrderMessageService orderMessageService;

  public OrderController(
      OrderService orderService,
      AuthService authService,
      OrderMessageService orderMessageService
  ) {
    this.orderService = orderService;
    this.authService = authService;
    this.orderMessageService = orderMessageService;
  }

  @PostMapping
  public Order create(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody Map<String, Object> body
  ) {
    UserEntity user = authService.requireUser(auth);
    return orderService.create(body, user.getId());
  }

  @GetMapping
  public Map<String, Object> list(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestParam(defaultValue = "50") int limit
  ) {
    String userId = authService.optionalUser(auth).map(UserEntity::getId).orElse(null);
    return Map.of("orders", orderService.list(limit, userId));
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable String id) {
    return orderService.getDetail(id);
  }

  @PostMapping("/{id}/cancellations")
  public Map<String, Object> cancel(@PathVariable String id) {
    Order o = orderService.cancel(id);
    return Map.of("id", o.getId(), "status", o.getStatus());
  }

  @PostMapping("/{id}/delivery-confirmations")
  public Map<String, Object> confirmDelivery(@PathVariable String id) {
    Order o = orderService.confirmDelivery(id);
    return Map.of(
        "id", o.getId(),
        "status", o.getStatus(),
        "status_label", o.getStatusLabel(),
        "message", "Delivery confirmed by customer"
    );
  }

  @PostMapping("/{id}/ratings")
  public Map<String, Object> rate(@PathVariable String id, @RequestBody Map<String, Object> body) {
    int rating = body.get("rating") instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(body.get("rating")));
    Order o = orderService.rate(id, rating);
    return Map.of("id", o.getId(), "status", o.getStatus(), "rating", o.getRating());
  }

  @PostMapping("/{id}/acceptances")
  public Map<String, Object> accept(@PathVariable String id) {
    Order o = orderService.advanceStatus(id, "confirmed");
    return Map.of("id", o.getId(), "status", o.getStatus(), "message", "Partner accepted (stub)");
  }

  @PatchMapping("/{id}/status")
  public Map<String, Object> status(@PathVariable String id, @RequestBody Map<String, Object> body) {
    String status = String.valueOf(body.get("status"));
    Order o = orderService.advanceStatus(id, status);
    return Map.of("id", o.getId(), "status", o.getStatus(), "status_label", o.getStatusLabel());
  }

  @GetMapping("/{id}/messages")
  public Map<String, Object> listMessages(@PathVariable String id) {
    Map<String, Object> detail = orderService.getDetail(id);
    return Map.of(
        "messages", orderMessageService.list(id),
        "order", detail
    );
  }

  @PostMapping("/{id}/messages")
  public Map<String, Object> sendMessage(@PathVariable String id, @RequestBody Map<String, Object> body) {
    // Fresh detail so assist replies match the live stepper / ETA
    Map<String, Object> detail = orderService.getDetail(id);
    Order order = orderService.get(id);
    String text = body.get("text") == null ? "" : String.valueOf(body.get("text")).trim();
    if (text.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "text is required");

    List<Map<String, Object>> messages = orderMessageService.sendCustomerMessage(order, text);
    // Re-read in case scheduler moved status mid-request
    detail = orderService.getDetail(id);
    return Map.of(
        "messages", messages,
        "order", detail
    );
  }
}
