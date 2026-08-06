package com.skywash.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.skywash.api.service.CatalogService;

@WebMvcTest(controllers = CatalogController.class)
class CatalogControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CatalogService catalogService;

  @Test
  void listServices() throws Exception {
    when(catalogService.listServices()).thenReturn(Map.of(
        "services", List.of(Map.of("type", "wash", "label", "Wash & Fold", "rate", 500)),
        "base_fee", 500,
        "platform_fee_rate", 0.10,
        "currency", "NGN"
    ));

    mockMvc.perform(get("/api/services"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("NGN"))
        .andExpect(jsonPath("$.services[0].type").value("wash"));
  }

  @Test
  void listPartners() throws Exception {
    when(catalogService.listPartners(eq("lekki"), isNull(), isNull(), isNull()))
        .thenReturn(List.of(Map.of("id", "p1", "name", "Laundry Care Lekki")));

    mockMvc.perform(get("/api/partners").param("q", "lekki"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partners[0].name").value("Laundry Care Lekki"));
  }

  @Test
  void nearby() throws Exception {
    when(catalogService.nearby(anyDouble(), anyDouble(), any(), any()))
        .thenReturn(Map.of(
            "pickup", Map.of("lat", 6.45, "lng", 3.47),
            "radius_km", 40.0,
            "offers", List.of()
        ));

    mockMvc.perform(get("/api/partners/nearby").param("lat", "6.45").param("lng", "3.47"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radius_km").value(40.0));
  }

  @Test
  void cities() throws Exception {
    when(catalogService.cities()).thenReturn(List.of("Abuja", "Lagos"));
    mockMvc.perform(get("/api/cities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cities[0]").value("Abuja"));
  }
}
