package com.skywash.api.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.skywash.api.config.ApiException;
import com.skywash.api.data.SeedData;
import com.skywash.api.model.Order;
import com.skywash.api.model.ServiceType;

@Service
public class PricingService {

  private final CatalogService catalogService;

  public PricingService(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  public Map<String, Object> validatePromo(String code) {
    String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    if ("SKY10".equals(normalized)) {
      return Map.of(
          "valid", true,
          "code", "SKY10",
          "rate", 0.10,
          "message", "10% off"
      );
    }
    return Map.of(
        "valid", false,
        "code", normalized,
        "rate", 0.0,
        "message", "Invalid code — try SKY10"
    );
  }

  public Map<String, Object> quote(List<Map<String, Object>> services, String promoCode) {
    if (services == null || services.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "At least one service is required");
    }

    List<Map<String, Object>> lineItems = new ArrayList<>();
    int serviceCost = 0;
    List<String> labelParts = new ArrayList<>();

    for (Map<String, Object> raw : services) {
      String type = String.valueOf(raw.get("type"));
      int qty = toInt(raw.get("qty"), 1);
      if (qty < 1 || qty > 20) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "qty must be between 1 and 20");
      }
      ServiceType st = catalogService.requireService(type);
      int amount = qty * st.rate();
      serviceCost += amount;
      labelParts.add(st.label() + " · " + qty + st.unit());
      Map<String, Object> line = new LinkedHashMap<>();
      line.put("type", st.type());
      line.put("label", st.label());
      line.put("qty", qty);
      line.put("unit", st.unit());
      line.put("rate", st.rate());
      line.put("amount", amount);
      lineItems.add(line);
    }

    Map<String, Object> promo = validatePromo(promoCode);
    boolean valid = Boolean.TRUE.equals(promo.get("valid"));
    double promoRate = valid ? ((Number) promo.get("rate")).doubleValue() : 0.0;

    int baseFee = SeedData.BASE_FEE;
    int platformFee = (int) Math.round(serviceCost * SeedData.PLATFORM_FEE_RATE);
    int subtotal = baseFee + serviceCost + platformFee;
    int discount = (int) Math.round(subtotal * promoRate);
    int total = subtotal - discount;

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("currency", "NGN");
    body.put("line_items", lineItems);
    body.put("base_fee", baseFee);
    body.put("service_cost", serviceCost);
    body.put("platform_fee", platformFee);
    body.put("promo", Map.of(
        "code", promo.get("code"),
        "rate", promoRate,
        "valid", valid
    ));
    body.put("discount", discount);
    body.put("total", total);
    body.put("label", String.join(" + ", labelParts));
    return body;
  }

  public Order.Pricing toPricing(Map<String, Object> quote) {
    return new Order.Pricing(
        (Integer) quote.get("base_fee"),
        (Integer) quote.get("service_cost"),
        (Integer) quote.get("platform_fee"),
        (Integer) quote.get("discount"),
        (Integer) quote.get("total"),
        (String) quote.get("currency")
    );
  }

  private static int toInt(Object v, int fallback) {
    if (v == null) return fallback;
    if (v instanceof Number n) return n.intValue();
    return Integer.parseInt(String.valueOf(v));
  }
}
