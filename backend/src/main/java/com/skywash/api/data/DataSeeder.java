package com.skywash.api.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.ServiceTypeEntity;
import com.skywash.api.model.ServiceType;
import com.skywash.api.repo.PartnerRepository;
import com.skywash.api.repo.ServiceTypeRepository;

@Component
public class DataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  private final PartnerRepository partnerRepository;
  private final ServiceTypeRepository serviceTypeRepository;

  public DataSeeder(PartnerRepository partnerRepository, ServiceTypeRepository serviceTypeRepository) {
    this.partnerRepository = partnerRepository;
    this.serviceTypeRepository = serviceTypeRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (serviceTypeRepository.count() == 0) {
      for (ServiceType s : SeedData.services()) {
        ServiceTypeEntity e = new ServiceTypeEntity();
        e.setType(s.type());
        e.setLabel(s.label());
        e.setRate(s.rate());
        e.setUnit(s.unit());
        e.setIcon(s.icon());
        serviceTypeRepository.save(e);
      }
      log.info("Seeded {} service types", SeedData.services().size());
    }

    // Pilot: when Neon has no partners (discovery empty), seed Lagos shops so
    // book → Paystack works. Nearby still caps at 25 km — use a Lagos pickup.
    if (partnerRepository.count() == 0) {
      for (SeedData.PilotPartner p : SeedData.pilotPartners()) {
        partnerRepository.save(toEntity(p));
      }
      log.info("Seeded {} pilot partners (Lagos)", SeedData.pilotPartners().size());
    } else {
      // Idempotent upsert of pilot ids so redeploys can refresh missing seed rows
      // without wiping live OSM discoveries.
      int added = 0;
      for (SeedData.PilotPartner p : SeedData.pilotPartners()) {
        if (partnerRepository.findById(p.id()).isEmpty()) {
          partnerRepository.save(toEntity(p));
          added++;
        }
      }
      if (added > 0) {
        log.info("Added {} missing pilot partners", added);
      }
    }

    for (var e : partnerRepository.findAll()) {
      if (e.getEmail() == null || e.getEmail().isBlank()) {
        String slug = e.getName() == null ? "partner" : e.getName().toLowerCase()
            .replaceAll("[^a-z0-9]+", ".")
            .replaceAll("^\\.|\\.$", "");
        if (slug.length() > 28) slug = slug.substring(0, 28).replaceAll("\\.$", "");
        e.setEmail("ops@" + slug + ".partner.skywash.app");
        partnerRepository.save(e);
      }
    }
  }

  private static PartnerEntity toEntity(SeedData.PilotPartner p) {
    PartnerEntity e = new PartnerEntity();
    e.setId(p.id());
    e.setName(p.name());
    e.setCity(p.city());
    e.setArea(p.area());
    e.setAddress(p.address());
    e.setLat(p.lat());
    e.setLng(p.lng());
    e.setRating(p.rating());
    e.setPhone(p.phone());
    e.setEmail("ops@" + p.id() + ".partner.skywash.app");
    e.setActive(true);
    return e;
  }
}
