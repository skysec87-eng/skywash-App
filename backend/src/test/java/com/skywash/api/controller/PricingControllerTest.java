package com.skywash.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

import com.skywash.api.service.GeocodeService;
import com.skywash.api.service.PricingService;

@WebMvcTest(controllers = PricingController.class)
class PricingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PricingService pricingService;
  @MockBean private GeocodeService geocodeService;

  @Test
  void quote() throws Exception {
    when(pricingService.quote(anyList(), eq("SKY10")))
        .thenReturn(Map.of("total", 1935, "currency", "NGN", "discount", 215));

    mockMvc.perform(post("/api/pricing/quote")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"services":[{"type":"wash","qty":3}],"promo_code":"SKY10"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1935));
  }

  @Test
  void validatePromo() throws Exception {
    when(pricingService.validatePromo("SKY10"))
        .thenReturn(Map.of("valid", true, "code", "SKY10", "rate", 0.10, "message", "10% off"));

    mockMvc.perform(post("/api/promos/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"SKY10\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true));
  }

  @Test
  void geocode() throws Exception {
    when(geocodeService.geocode(any()))
        .thenReturn(Map.of("lat", 6.45, "lng", 3.47, "formatted_address", "Sangotedo, Lagos, Nigeria", "demo", true));

    mockMvc.perform(post("/api/geocode")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"address\":\"Sangotedo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.demo").value(true))
        .andExpect(jsonPath("$.lat").value(6.45));
  }
}
