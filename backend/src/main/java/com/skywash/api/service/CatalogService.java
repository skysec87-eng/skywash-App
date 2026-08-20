package com.skywash.api.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.skywash.api.config.ApiException;
import com.skywash.api.data.SeedData;
import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.ServiceTypeEntity;
import com.skywash.api.model.ServiceType;
import com.skywash.api.repo.PartnerRepository;
import com.skywash.api.repo.ServiceTypeRepository;
import com.skywash.api.util.GeoUtils;
import com.skywash.api.util.TripEtaCalculator;

@Service
public class CatalogService {

  private final PartnerRepository partnerRepository;
  private final ServiceTypeRepository serviceTypeRepository;

  public CatalogService(PartnerRepository partnerRepository, ServiceTypeRepository serviceTypeRepository) {
    this.partnerRepository = partnerRepository;
    this.serviceTypeRepository = serviceTypeRepository;
  }

  public Map<String, Object> listServices() {
    List<Map<String, Object>> services = serviceTypeRepository.findAll().stream()
        .map(s -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("type", s.getType());
          m.put("label", s.getLabel());
          m.put("rate", s.getRate());
          m.put("unit", s.getUnit());
          m.put("icon", s.getIcon());
          return m;
        })
        .collect(Collectors.toList());
    return Map.of(
        "services", services,
        "base_fee", SeedData.BASE_FEE,
        "platform_fee_rate", SeedData.PLATFORM_FEE_RATE,
        "currency", "NGN"
    );
  }

  public ServiceType requireService(String type) {
    ServiceTypeEntity s = serviceTypeRepository.findById(type)
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown service type: " + type));
    return new ServiceType(s.getType(), s.getLabel(), s.getRate(), s.getUnit(), s.getIcon());
  }

  public List<Map<String, Object>> listPartners(String q, String city, Double lat, Double lng) {
    String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
    String cityFilter = city == null ? "" : city.trim().toLowerCase(Locale.ROOT);

    return partnerRepository.findByActiveTrue().stream()
        .filter(p -> cityFilter.isEmpty() || p.getCity().toLowerCase(Locale.ROOT).equals(cityFilter))
        .filter(p -> term.isEmpty()
            || p.getName().toLowerCase(Locale.ROOT).contains(term)
            || p.getArea().toLowerCase(Locale.ROOT).contains(term)
            || p.getCity().toLowerCase(Locale.ROOT).contains(term))
        .map(p -> toPartnerMap(p, lat, lng))
        .collect(Collectors.toList());
  }

  public Map<String, Object> nearby(double lat, double lng, Double radiusKm, Integer limit) {
    return nearby(lat, lng, radiusKm, limit, null, 2);
  }

  /**
   * @param serviceType optional primary service (wash|dry|iron|express); default wash
   * @param qty bag size / item count used for wash timing
   */
  public Map<String, Object> nearby(
      double lat, double lng, Double radiusKm, Integer limit, String serviceType, Integer qty
  ) {
    double radius = radiusKm == null ? SeedData.NEARBY_RADIUS_KM : radiusKm;
    int lim = limit == null ? SeedData.NEARBY_LIMIT : limit;
    String svc = serviceType == null || serviceType.isBlank() ? "wash" : serviceType.trim();
    int q = qty == null || qty < 1 ? 2 : qty;

    List<Map<String, Object>> offers = partnerRepository.findByActiveTrue().stream()
        .map(p -> Map.entry(p, GeoUtils.haversineKm(lat, lng, p.getLat(), p.getLng())))
        .filter(e -> e.getValue() <= radius)
        .sorted(Comparator.comparingDouble(Map.Entry::getValue))
        .limit(lim)
        .map(e -> {
          PartnerEntity p = e.getKey();
          double dist = e.getValue();
          var plan = TripEtaCalculator.plan(dist, svc, q);
          Map<String, Object> offer = new LinkedHashMap<>();
          offer.put("partner", toPartnerMap(p, null, null));
          offer.put("distance_km", round1(dist));
          offer.put("eta_minutes", plan.pickupTravelMin()); // time to pickup
          offer.put("total_eta_minutes", plan.totalMin());
          offer.put("eta_breakdown", plan.toMap());
          return offer;
        })
        .collect(Collectors.toList());

    return Map.of(
        "pickup", Map.of("lat", lat, "lng", lng),
        "radius_km", radius,
        "offers", offers
    );
  }

  public PartnerEntity requirePartner(String id) {
    return partnerRepository.findById(id)
        .filter(PartnerEntity::isActive)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partner not found: " + id));
  }

  public Optional<PartnerEntity> findById(String id) {
    return partnerRepository.findById(id);
  }

  public List<String> cities() {
    return partnerRepository.findByActiveTrue().stream()
        .map(PartnerEntity::getCity)
        .distinct()
        .sorted()
        .toList();
  }

  private Map<String, Object> toPartnerMap(PartnerEntity p, Double lat, Double lng) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", p.getId());
    m.put("name", p.getName());
    m.put("city", p.getCity());
    m.put("area", p.getArea());
    m.put("address", p.getAddress());
    m.put("lat", p.getLat());
    m.put("lng", p.getLng());
    m.put("rating", p.getRating());
    m.put("phone", p.getPhone());
    if (lat != null && lng != null) {
      m.put("distance_km", round1(GeoUtils.haversineKm(lat, lng, p.getLat(), p.getLng())));
    }
    return m;
  }

  private static double round1(double v) {
    return Math.round(v * 10.0) / 10.0;
  }
}
