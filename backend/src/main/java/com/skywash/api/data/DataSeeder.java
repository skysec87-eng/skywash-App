package com.skywash.api.data;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skywash.api.entity.ServiceTypeEntity;
import com.skywash.api.model.ServiceType;
import com.skywash.api.repo.PartnerRepository;
import com.skywash.api.repo.ServiceTypeRepository;

@Component
public class DataSeeder implements ApplicationRunner {

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
    }
    // Partners are discovered live around each pickup (OSM / Google Places), not seeded.
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
}
