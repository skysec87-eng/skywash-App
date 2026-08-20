package com.skywash.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skywash.api.config.ApiException;
import com.skywash.api.service.OpsDashboardService;

@RestController
@RequestMapping("/api/ops")
public class OpsController {

  private final OpsDashboardService opsDashboardService;
  private final String opsPin;

  public OpsController(
      OpsDashboardService opsDashboardService,
      @Value("${ops.pin:}") String opsPin
  ) {
    this.opsDashboardService = opsDashboardService;
    this.opsPin = opsPin == null ? "" : opsPin.trim();
  }

  @GetMapping("/partners")
  public Map<String, Object> partners(@RequestHeader(value = "X-Ops-Pin", required = false) String pin) {
    requirePin(pin);
    return Map.of("partners", opsDashboardService.listPartners());
  }

  @GetMapping("/orders")
  public Map<String, Object> orders(
      @RequestHeader(value = "X-Ops-Pin", required = false) String pin,
      @RequestParam String partner_id,
      @RequestParam(defaultValue = "40") int limit
  ) {
    requirePin(pin);
    return opsDashboardService.listOrders(partner_id, limit);
  }

  private void requirePin(String pin) {
    if (!StringUtils.hasText(opsPin)) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Partner ops is not configured (set OPS_PIN on the server)"
      );
    }
    if (!opsPin.equals(pin == null ? "" : pin.trim())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid ops PIN");
    }
  }
}
