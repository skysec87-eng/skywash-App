package com.skywash.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private static String partnerId;
  private static String orderId;
  private static String authToken;

  @Test
  @Order(1)
  void healthIsUp() throws Exception {
    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.service").value("skywash-api"));
  }

  @Test
  @Order(2)
  void servicesAreSeeded() throws Exception {
    mockMvc.perform(get("/api/services"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("NGN"))
        .andExpect(jsonPath("$.base_fee").value(500))
        .andExpect(jsonPath("$.services", hasSize(4)))
        .andExpect(jsonPath("$.services[0].type").exists());
  }

  @Test
  @Order(3)
  void partnersAndCitiesAreSeeded() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/partners"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partners", hasSize(greaterThan(0))))
        .andReturn();

    JsonNode partners = objectMapper.readTree(result.getResponse().getContentAsString()).get("partners");
    partnerId = partners.get(0).get("id").asText();
    assertThat(partnerId).isNotBlank();

    mockMvc.perform(get("/api/cities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cities", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @Order(4)
  void nearbyReturnsOffersAroundLagos() throws Exception {
    mockMvc.perform(get("/api/partners/nearby")
            .param("lat", "6.45")
            .param("lng", "3.47")
            .param("radius_km", "40")
            .param("limit", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.offers", hasSize(greaterThan(0))))
        .andExpect(jsonPath("$.offers[0].partner.id").exists())
        .andExpect(jsonPath("$.offers[0].distance_km").exists())
        .andExpect(jsonPath("$.offers[0].eta_minutes").exists());
  }

  @Test
  @Order(5)
  void quoteAndPromoAndGeocode() throws Exception {
    mockMvc.perform(post("/api/pricing/quote")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"services":[{"type":"wash","qty":3}],"promo_code":"SKY10"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1935))
        .andExpect(jsonPath("$.discount").value(215));

    mockMvc.perform(post("/api/promos/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"SKY10\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true));

    mockMvc.perform(post("/api/geocode")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"address\":\"Lekki Phase 1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lat").exists())
        .andExpect(jsonPath("$.lng").exists())
        .andExpect(jsonPath("$.demo").value(true));
  }

  @Test
  @Order(6)
  void createOrderThenFetchDetailAndList() throws Exception {
    assertThat(partnerId).isNotBlank();

    String phone = "+234802" + System.currentTimeMillis() % 10000000;
    MvcResult signup = mockMvc.perform(post("/api/auth/password-registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Order User\",\"phone\":\"" + phone + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    authToken = objectMapper.readTree(signup.getResponse().getContentAsString()).get("token").asText();

    MvcResult nearby = mockMvc.perform(get("/api/partners/nearby")
            .param("lat", "6.45")
            .param("lng", "3.47"))
        .andExpect(status().isOk())
        .andReturn();
    partnerId = objectMapper.readTree(nearby.getResponse().getContentAsString())
        .path("offers").get(0).path("partner").path("id").asText();

    String body = """
        {
          "partner_id":"%s",
          "pickup":{"lat":6.45,"lng":3.47,"address":"Sangotedo"},
          "services":[{"type":"wash","qty":3}],
          "payment_method":"card",
          "promo_code":"SKY10"
        }
        """.formatted(partnerId);

    MvcResult created = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("confirmed"))
        .andExpect(jsonPath("$.pricing.total").value(1935))
        .andReturn();

    orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    assertThat(orderId).isNotBlank();

    mockMvc.perform(get("/api/orders/" + orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId))
        .andExpect(jsonPath("$.status").value("confirmed"))
        .andExpect(jsonPath("$.pricing.total").value(1935))
        .andExpect(jsonPath("$.partner.id").value(partnerId));

    mockMvc.perform(get("/api/orders").param("limit", "10")
            .header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orders", hasSize(greaterThan(0))))
        .andExpect(jsonPath("$.orders[0].id").exists());
  }

  @Test
  @Order(7)
  void chatAcceptStatusAndPayment() throws Exception {
    assertThat(orderId).isNotBlank();

    mockMvc.perform(post("/api/orders/" + orderId + "/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\":\"Please ring the bell\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages", hasSize(greaterThanOrEqualTo(3))));

    mockMvc.perform(get("/api/orders/" + orderId + "/messages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages", hasSize(greaterThanOrEqualTo(3))));

    mockMvc.perform(post("/api/orders/" + orderId + "/acceptances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("confirmed"));

    mockMvc.perform(patch("/api/orders/" + orderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"enroute\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("enroute"));

    mockMvc.perform(post("/api/payments/checkouts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"order_id\":\"" + orderId + "\",\"amount\":1935}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.provider").value("paystack"));

    mockMvc.perform(post("/api/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"event\":\"charge.success\",\"data\":{\"reference\":\"SKY_x\",\"status\":\"success\",\"metadata\":{\"order_id\":\"" + orderId + "\"}}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.received").value(true));

    mockMvc.perform(post("/api/couriers/demo-courier/location")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"lat\":6.451,\"lng\":3.471}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  @Test
  @Order(8)
  void authMeAndProfileUpdate() throws Exception {
    String phone = "+234801" + System.currentTimeMillis() % 10000000;
    String email = "otp" + (System.currentTimeMillis() % 10000000) + "@example.com";

    MvcResult sent = mockMvc.perform(post("/api/auth/verification-codes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.debug_code").isNotEmpty())
        .andReturn();

    String code = objectMapper.readTree(sent.getResponse().getContentAsString()).get("debug_code").asText();

    MvcResult verified = mockMvc.perform(post("/api/auth/verification-codes/confirmations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registration_required").value(true))
        .andExpect(jsonPath("$.registration_token").isNotEmpty())
        .andReturn();

    String profileToken = objectMapper.readTree(verified.getResponse().getContentAsString())
        .get("registration_token").asText();

    MvcResult completed = mockMvc.perform(post("/api/auth/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"registration_token\":\"" + profileToken + "\",\"name\":\"Integration Tester\",\"phone\":\"" + phone + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andReturn();

    // Use a local token so logout here does not break later tests that share authToken
    String token = objectMapper.readTree(completed.getResponse().getContentAsString()).get("token").asText();

    mockMvc.perform(get("/api/account").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.phone").value(phone))
        .andExpect(jsonPath("$.user.email").value(email));

    mockMvc.perform(patch("/api/account")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Updated Name\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.name").value("Updated Name"));

    mockMvc.perform(delete("/api/auth/sessions/current").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  @Order(9)
  void rateThenCannotCancel() throws Exception {
    assertThat(orderId).isNotBlank();

    mockMvc.perform(patch("/api/orders/" + orderId + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"delivering\"}"))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/orders/" + orderId + "/delivery-confirmations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("delivered"));

    mockMvc.perform(post("/api/orders/" + orderId + "/ratings")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"rating\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("rated"))
        .andExpect(jsonPath("$.rating").value(5));

    mockMvc.perform(post("/api/orders/" + orderId + "/cancellations"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  @Order(10)
  void cancelFreshOrderWorks() throws Exception {
    assertThat(partnerId).isNotBlank();
    assertThat(authToken).isNotBlank();

    String body = """
        {
          "partner_id":"%s",
          "pickup":{"lat":6.45,"lng":3.47,"address":"Ajah"},
          "services":[{"type":"iron","qty":2}],
          "payment_method":"cash"
        }
        """.formatted(partnerId);

    MvcResult created = mockMvc.perform(post("/api/orders")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andReturn();

    String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

    mockMvc.perform(post("/api/orders/" + id + "/cancellations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"));
  }
}
