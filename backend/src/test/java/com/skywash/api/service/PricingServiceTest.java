package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skywash.api.config.ApiException;
import com.skywash.api.model.Order;
import com.skywash.api.model.ServiceType;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

  @Mock
  private CatalogService catalogService;

  private PricingService pricingService;

  @BeforeEach
  void setUp() {
    pricingService = new PricingService(catalogService);
  }

  @Test
  void validatePromoSky10() {
    Map<String, Object> result = pricingService.validatePromo(" sky10 ");
    assertEquals(true, result.get("valid"));
    assertEquals("SKY10", result.get("code"));
    assertEquals(0.10, ((Number) result.get("rate")).doubleValue(), 1e-9);
  }

  @Test
  void validatePromoInvalid() {
    Map<String, Object> result = pricingService.validatePromo("NOPE");
    assertFalse((Boolean) result.get("valid"));
    assertEquals(0.0, ((Number) result.get("rate")).doubleValue(), 1e-9);
  }

  @Test
  void quoteRequiresServices() {
    assertThrows(ApiException.class, () -> pricingService.quote(List.of(), null));
    assertThrows(ApiException.class, () -> pricingService.quote(null, null));
  }

  @Test
  void quoteRejectsBadQty() {
    // qty is validated before catalog lookup
    assertThrows(ApiException.class,
        () -> pricingService.quote(List.of(Map.of("type", "wash", "qty", 0)), null));
    assertThrows(ApiException.class,
        () -> pricingService.quote(List.of(Map.of("type", "wash", "qty", 21)), null));
  }

  @Test
  void quoteWithoutPromoMatchesFormula() {
    when(catalogService.requireService("wash"))
        .thenReturn(new ServiceType("wash", "Wash & Fold", 500, "kg", "🧺"));

    Map<String, Object> quote = pricingService.quote(
        List.of(Map.of("type", "wash", "qty", 3)), null);

    // service=1500, base=500, platform=150, total=2150
    assertEquals(500, quote.get("base_fee"));
    assertEquals(1500, quote.get("service_cost"));
    assertEquals(150, quote.get("platform_fee"));
    assertEquals(0, quote.get("discount"));
    assertEquals(2150, quote.get("total"));
    assertEquals("NGN", quote.get("currency"));
  }

  @Test
  void quoteWithSky10AppliesTenPercent() {
    when(catalogService.requireService("wash"))
        .thenReturn(new ServiceType("wash", "Wash & Fold", 500, "kg", "🧺"));

    Map<String, Object> quote = pricingService.quote(
        List.of(Map.of("type", "wash", "qty", 3)), "SKY10");

    // subtotal 2150, discount 215, total 1935
    assertEquals(215, quote.get("discount"));
    assertEquals(1935, quote.get("total"));
    assertTrue((Boolean) ((Map<?, ?>) quote.get("promo")).get("valid"));
  }

  @Test
  void toPricingMapsQuoteFields() {
    Map<String, Object> quote = Map.of(
        "base_fee", 500,
        "service_cost", 1500,
        "platform_fee", 150,
        "discount", 215,
        "total", 1935,
        "currency", "NGN"
    );
    Order.Pricing pricing = pricingService.toPricing(quote);
    assertEquals(500, pricing.baseFee());
    assertEquals(1935, pricing.total());
    assertEquals("NGN", pricing.currency());
  }
}
