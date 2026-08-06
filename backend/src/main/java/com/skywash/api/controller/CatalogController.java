package com.skywash.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skywash.api.service.CatalogService;

@RestController
@RequestMapping("/api")
public class CatalogController {

  private final CatalogService catalogService;

  public CatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/services")
  public Map<String, Object> services() {
    return catalogService.listServices();
  }

  @GetMapping("/partners")
  public Map<String, Object> partners(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) Double lat,
      @RequestParam(required = false) Double lng
  ) {
    List<Map<String, Object>> partners = catalogService.listPartners(q, city, lat, lng);
    return Map.of("partners", partners);
  }

  @GetMapping("/partners/nearby")
  public Map<String, Object> nearby(
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(required = false) Double radius_km,
      @RequestParam(required = false) Integer limit
  ) {
    return catalogService.nearby(lat, lng, radius_km, limit);
  }

  @GetMapping("/cities")
  public Map<String, Object> cities() {
    return Map.of("cities", catalogService.cities());
  }
}
