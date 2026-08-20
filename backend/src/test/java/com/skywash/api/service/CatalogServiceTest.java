package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skywash.api.config.ApiException;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.ServiceTypeEntity;
import com.skywash.api.repo.PartnerRepository;
import com.skywash.api.repo.ServiceTypeRepository;
import com.skywash.api.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

  @Mock private PartnerRepository partnerRepository;
  @Mock private ServiceTypeRepository serviceTypeRepository;

  private CatalogService catalogService;

  private PartnerEntity lekki;
  private PartnerEntity abuja;

  @BeforeEach
  void setUp() {
    catalogService = new CatalogService(partnerRepository, serviceTypeRepository);
    lekki = TestFixtures.partner("p1", "Laundry Care Lekki", "Lagos", "Lekki", 6.450511, 3.4704056, 5.0);
    abuja = TestFixtures.partner("p2", "Capital Wash Hub", "Abuja", "Wuse", 9.0765, 7.3986, 4.7);
  }

  @Test
  void listServicesIncludesFeeConstants() {
    ServiceTypeEntity wash = TestFixtures.service("wash", "Wash & Fold", 500, "kg", "🧺");
    when(serviceTypeRepository.findAll()).thenReturn(List.of(wash));

    Map<String, Object> body = catalogService.listServices();
    assertEquals(500, body.get("base_fee"));
    assertEquals(0.10, ((Number) body.get("platform_fee_rate")).doubleValue(), 1e-9);
    assertEquals(1, ((List<?>) body.get("services")).size());
  }

  @Test
  void requireServiceUnknownThrows() {
    when(serviceTypeRepository.findById("nope")).thenReturn(Optional.empty());
    assertThrows(ApiException.class, () -> catalogService.requireService("nope"));
  }

  @Test
  void listPartnersFiltersByCityAndQuery() {
    when(partnerRepository.findByActiveTrue()).thenReturn(List.of(lekki, abuja));

    List<Map<String, Object>> lagos = catalogService.listPartners(null, "Lagos", null, null);
    assertEquals(1, lagos.size());
    assertEquals("Laundry Care Lekki", lagos.get(0).get("name"));

    List<Map<String, Object>> search = catalogService.listPartners("wuse", null, null, null);
    assertEquals(1, search.size());
    assertEquals("Capital Wash Hub", search.get(0).get("name"));
  }

  @Test
  void nearbyReturnsSortedOffersWithinRadius() {
    when(partnerRepository.findByActiveTrue()).thenReturn(List.of(lekki, abuja));

    Map<String, Object> nearby = catalogService.nearby(6.45, 3.47, 40.0, 8);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> offers = (List<Map<String, Object>>) nearby.get("offers");

    assertEquals(1, offers.size());
    assertEquals(40.0, nearby.get("radius_km"));
    assertEquals(false, nearby.get("expanded"));
    assertTrue(((Number) offers.get(0).get("distance_km")).doubleValue() < 5);
  }

  @Test
  void nearbyExpandsPastFortyKmToNearestPartners() {
    when(partnerRepository.findByActiveTrue()).thenReturn(List.of(lekki, abuja));

    // Cotonou-ish pickup — nothing is within 40 km of Lagos/Abuja shops.
    Map<String, Object> nearby = catalogService.nearby(6.3654, 2.4280, 40.0, 8);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> offers = (List<Map<String, Object>>) nearby.get("offers");

    assertEquals(true, nearby.get("expanded"));
    assertEquals(2, offers.size());
    assertTrue(((Number) offers.get(0).get("distance_km")).doubleValue() > 40);
  }

  @Test
  void nearbyWithoutRadiusReturnsNearestUncapped() {
    when(partnerRepository.findByActiveTrue()).thenReturn(List.of(lekki, abuja));

    Map<String, Object> nearby = catalogService.nearby(6.3654, 2.4280, null, 8);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> offers = (List<Map<String, Object>>) nearby.get("offers");

    assertEquals(false, nearby.get("expanded"));
    assertEquals(2, offers.size());
  }

  @Test
  void citiesAreDistinctAndSorted() {
    when(partnerRepository.findByActiveTrue()).thenReturn(List.of(lekki, abuja, lekki));
    assertEquals(List.of("Abuja", "Lagos"), catalogService.cities());
  }

  @Test
  void requirePartnerInactiveThrows() {
    PartnerEntity inactive = TestFixtures.partner("x", "X", "Lagos", "Yaba", 6.5, 3.3, 4.0);
    inactive.setActive(false);
    when(partnerRepository.findById("x")).thenReturn(Optional.of(inactive));
    assertThrows(ApiException.class, () -> catalogService.requirePartner("x"));
  }
}
