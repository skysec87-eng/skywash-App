package com.skywash.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.skywash.api.entity.UserEntity;
import com.skywash.api.model.Order;
import com.skywash.api.service.AuthService;
import com.skywash.api.service.OrderService;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OrderService orderService;
  @MockBean private AuthService authService;
  @MockBean private com.skywash.api.service.OrderMessageService orderMessageService;

  @Test
  void createOrder() throws Exception {
    UserEntity user = new UserEntity();
    user.setId("u1");
    user.setName("Ada");
    when(authService.requireUser(any())).thenReturn(user);
    Order order = new Order();
    order.setId("o1");
    order.setStatus("confirmed");
    order.setPartnerId("p1");
    when(orderService.create(any(), eq("u1"))).thenReturn(order);

    mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer tok")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "partner_id":"p1",
                  "pickup":{"lat":6.45,"lng":3.47,"address":"Sangotedo"},
                  "services":[{"type":"wash","qty":3}],
                  "payment_method":"card"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("o1"))
        .andExpect(jsonPath("$.status").value("confirmed"));
  }

  @Test
  void listOrders() throws Exception {
    UserEntity user = new UserEntity();
    user.setId("u1");
    when(authService.optionalUser(any())).thenReturn(java.util.Optional.of(user));
    when(orderService.list(anyInt(), eq("u1"))).thenReturn(List.of(
        Map.of("id", "o1", "provider_name", "Laundry Care Lekki", "total", 1935)
    ));

    mockMvc.perform(get("/api/orders").header("Authorization", "Bearer tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orders[0].id").value("o1"));
  }

  @Test
  void getOrderDetail() throws Exception {
    when(orderService.getDetail("o1")).thenReturn(Map.of(
        "id", "o1",
        "status", "confirmed",
        "status_label", "Request confirmed"
    ));

    mockMvc.perform(get("/api/orders/o1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("confirmed"));
  }

  @Test
  void cancelOrder() throws Exception {
    Order o = new Order();
    o.setId("o1");
    o.setStatus("cancelled");
    when(orderService.cancel("o1")).thenReturn(o);

    mockMvc.perform(post("/api/orders/o1/cancellations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"));
  }

  @Test
  void rateOrder() throws Exception {
    Order o = new Order();
    o.setId("o1");
    o.setStatus("rated");
    o.setRating(5);
    when(orderService.rate(eq("o1"), eq(5))).thenReturn(o);

    mockMvc.perform(post("/api/orders/o1/ratings")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"rating\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rating").value(5));
  }

  @Test
  void acceptOrder() throws Exception {
    Order o = new Order();
    o.setId("o1");
    o.setStatus("confirmed");
    when(orderService.advanceStatus("o1", "confirmed")).thenReturn(o);

    mockMvc.perform(post("/api/orders/o1/acceptances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("confirmed"));
  }

  @Test
  void updateStatus() throws Exception {
    Order o = new Order();
    o.setId("o1");
    o.setStatus("enroute");
    o.setStatusLabel("Partner heading to you");
    when(orderService.advanceStatus("o1", "enroute")).thenReturn(o);

    mockMvc.perform(patch("/api/orders/o1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"enroute\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("enroute"));
  }

  @Test
  void chatRoundTrip() throws Exception {
    Order o = new Order();
    o.setId("o1");
    o.setStatus("confirmed");
    o.setStatusLabel("Request confirmed");
    o.setProviderName("Dee Clean");
    when(orderService.get("o1")).thenReturn(o);
    when(orderMessageService.list("o1")).thenReturn(List.of());
    when(orderMessageService.sendCustomerMessage(any(), any()))
        .thenReturn(List.of(
            Map.of("sender", "customer", "text", "Ring the bell"),
            Map.of("sender", "assist", "text", "Sure — I'll collect from security.")
        ));
    when(orderService.getDetail("o1")).thenReturn(Map.of("id", "o1", "status", "confirmed"));
    when(orderService.get("o1")).thenReturn(o);

    mockMvc.perform(get("/api/orders/o1/messages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages").isArray())
        .andExpect(jsonPath("$.order.status").value("confirmed"));

    mockMvc.perform(post("/api/orders/o1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\":\"Ring the bell\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages.length()").value(2))
        .andExpect(jsonPath("$.messages[0].text").value("Ring the bell"))
        .andExpect(jsonPath("$.messages[1].sender").value("assist"))
        .andExpect(jsonPath("$.order.id").value("o1"));
  }
}
