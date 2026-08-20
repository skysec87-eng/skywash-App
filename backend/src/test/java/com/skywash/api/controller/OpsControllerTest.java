package com.skywash.api.controller;

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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.skywash.api.config.ApiExceptionHandler;
import com.skywash.api.service.OpsDashboardService;

@WebMvcTest(controllers = OpsController.class)
@Import(ApiExceptionHandler.class)
@TestPropertySource(properties = "ops.pin=test-pin")
class OpsControllerTest {

  @Autowired MockMvc mockMvc;
  @MockBean OpsDashboardService opsDashboardService;

  @Test
  void rejectsMissingPin() throws Exception {
    mockMvc.perform(get("/api/ops/partners"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listsPartnersWithValidPin() throws Exception {
    when(opsDashboardService.listPartners()).thenReturn(List.of(
        Map.of("id", "p1", "name", "Laundry Care Lekki", "city", "Lagos", "area", "Lekki")
    ));
    mockMvc.perform(get("/api/ops/partners").header("X-Ops-Pin", "test-pin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partners[0].name").value("Laundry Care Lekki"));
  }
}
