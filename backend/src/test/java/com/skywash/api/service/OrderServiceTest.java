package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skywash.api.config.ApiException;
import com.skywash.api.entity.OrderEntity;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.model.Order;
import com.skywash.api.repo.OrderRepository;
import com.skywash.api.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private CatalogService catalogService;
  @Mock private PricingService pricingService;
  @Mock private OrderRepository orderRepository;
  @Mock private OrderMessageService orderMessageService;

  private OrderService orderService;
  private PartnerEntity partner;

  @BeforeEach
  void setUp() {
    orderService = new OrderService(catalogService, pricingService, orderRepository, orderMessageService);
    partner = TestFixtures.partner("p1", "Laundry Care Lekki", "Lagos", "Lekki", 6.450511, 3.4704056, 5.0);
  }

  @Test
  void createPersistsConfirmedOrder() {
    when(catalogService.requirePartner("p1")).thenReturn(partner);
    when(pricingService.quote(any(), any())).thenReturn(Map.of(
        "base_fee", 500,
        "service_cost", 1500,
        "platform_fee", 150,
        "discount", 215,
        "total", 1935,
        "currency", "NGN",
        "label", "Wash & Fold · 3kg"
    ));
    when(pricingService.toPricing(any())).thenReturn(
        new Order.Pricing(500, 1500, 150, 215, 1935, "NGN"));
    when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> body = new HashMap<>();
    body.put("partner_id", "p1");
    body.put("pickup", Map.of("lat", 6.45, "lng", 3.47, "address", "Sangotedo"));
    body.put("services", List.of(Map.of("type", "wash", "qty", 3)));
    body.put("payment_method", "card");
    body.put("promo_code", "SKY10");

    Order created = orderService.create(body, "user-1");

    assertEquals("confirmed", created.getStatus());
    assertEquals("p1", created.getPartnerId());
    assertEquals(1935, created.getPricing().total());
    assertEquals("SKY10", created.getPromoCode());

    ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
    verify(orderRepository).save(captor.capture());
    assertEquals("Laundry Care Lekki", captor.getValue().getProviderName());
    assertEquals("user-1", captor.getValue().getUserId());
  }

  @Test
  void createRequiresLogin() {
    Map<String, Object> body = Map.of("partner_id", "p1");
    assertThrows(ApiException.class, () -> orderService.create(body, null));
  }

  @Test
  void createRequiresPickup() {
    when(catalogService.requirePartner("p1")).thenReturn(partner);
    Map<String, Object> body = Map.of(
        "partner_id", "p1",
        "services", List.of(Map.of("type", "wash", "qty", 1))
    );
    assertThrows(ApiException.class, () -> orderService.create(body, "user-1"));
  }

  @Test
  void cancelActiveOrder() {
    OrderEntity o = TestFixtures.order("o1", "p1", "confirmed");
    when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
    when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Order cancelled = orderService.cancel("o1");
    assertEquals("cancelled", cancelled.getStatus());
  }

  @Test
  void cancelDeliveredThrows() {
    OrderEntity o = TestFixtures.order("o1", "p1", "delivered");
    when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
    assertThrows(ApiException.class, () -> orderService.cancel("o1"));
  }

  @Test
  void rateValidatesRange() {
    // rating bounds are checked before loading the order
    assertThrows(ApiException.class, () -> orderService.rate("o1", 0));
    assertThrows(ApiException.class, () -> orderService.rate("o1", 6));
  }

  @Test
  void rateSetsRatedStatus() {
    OrderEntity o = TestFixtures.order("o1", "p1", "delivered");
    when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
    when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Order rated = orderService.rate("o1", 5);
    assertEquals("rated", rated.getStatus());
    assertEquals(5, rated.getRating());
  }

  @Test
  void getDetailIncludesPricingAndPartner() {
    OrderEntity o = TestFixtures.order("o1", "p1", "confirmed");
    when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
    when(catalogService.findById("p1")).thenReturn(Optional.of(partner));

    Map<String, Object> detail = orderService.getDetail("o1");
    assertEquals("o1", detail.get("id"));
    assertEquals("confirmed", detail.get("status"));
    assertTrue(String.valueOf(detail.get("eta_label")).contains("min")
        || "Scheduled".equals(detail.get("eta_label")));
    @SuppressWarnings("unchecked")
    Map<String, Object> p = (Map<String, Object>) detail.get("partner");
    assertEquals("Laundry Care Lekki", p.get("name"));
  }

  @Test
  void progressActiveOrdersAdvancesWhenDurationElapsed() {
    OrderEntity o = TestFixtures.order("o1", "p1", "confirmed");
    o.getTimeline().clear();
    o.getTimeline().add(new OrderEntity.TimelineEmbed("confirmed", "Request confirmed",
        Instant.now().minusSeconds(10)));
    when(orderRepository.findByStatusNotIn(any())).thenReturn(List.of(o));
    when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(catalogService.findById("p1")).thenReturn(Optional.of(partner));

    orderService.progressActiveOrders();

    assertEquals("enroute", o.getStatus());
    verify(orderRepository).save(o);
    verify(orderMessageService).announceStatus(o, "enroute");
  }

  @Test
  void advanceStatusRejectsUnknown() {
    OrderEntity o = TestFixtures.order("o1", "p1", "confirmed");
    when(orderRepository.findById("o1")).thenReturn(Optional.of(o));
    assertThrows(ApiException.class, () -> orderService.advanceStatus("o1", "flying"));
  }
}
