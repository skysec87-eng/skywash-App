package com.skywash.api.data;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skywash.api.entity.PartnerEntity;
import com.skywash.api.entity.ServiceTypeEntity;
import com.skywash.api.model.Partner;
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
    if (partnerRepository.count() == 0) {
      for (Partner p : SeedData.partners()) {
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
        e.setActive(p.isActive());
        partnerRepository.save(e);
      }
    }
  }
}
