package com.skywash.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skywash.api.service.GeocodeService;
import com.skywash.api.service.PricingService;

@RestController
@RequestMapping("/api")
public class PricingController {

  private final PricingService pricingService;
  private final GeocodeService geocodeService;

  public PricingController(PricingService pricingService, GeocodeService geocodeService) {
    this.pricingService = pricingService;
    this.geocodeService = geocodeService;
  }

  @PostMapping("/pricing/quote")
  public Map<String, Object> quote(@RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> services = (List<Map<String, Object>>) body.get("services");
    String promo = body.get("promo_code") == null ? null : String.valueOf(body.get("promo_code"));
    return pricingService.quote(services, promo);
  }

  @PostMapping("/promos/validate")
  public Map<String, Object> validatePromo(@RequestBody Map<String, Object> body) {
    String code = body.get("code") == null ? "" : String.valueOf(body.get("code"));
    return pricingService.validatePromo(code);
  }

  @PostMapping("/geocode")
  public Map<String, Object> geocode(@RequestBody Map<String, Object> body) {
    String address = body.get("address") == null ? "" : String.valueOf(body.get("address"));
    return geocodeService.geocode(address);
  }
}
