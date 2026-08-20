package com.skywash.api.data;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skywash.api.repo.PartnerRepository;
import com.skywash.api.support.TestFixtures;

@Component
@Profile("test")
@Order(2)
class TestPartnerSeeder implements ApplicationRunner {

  private final PartnerRepository partnerRepository;

  TestPartnerSeeder(PartnerRepository partnerRepository) {
    this.partnerRepository = partnerRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (partnerRepository.count() > 0) return;
    partnerRepository.save(TestFixtures.partner(
        "test-lekki", "Test Lekki Laundry", "Lagos", "Lekki", 6.450511, 3.4704056, 5.0));
    partnerRepository.save(TestFixtures.partner(
        "test-yaba", "Test Yaba Laundry", "Lagos", "Yaba", 6.5058, 3.3780, 4.6));
    partnerRepository.save(TestFixtures.partner(
        "test-wuse", "Test Wuse Laundry", "Abuja", "Wuse", 9.0765, 7.3986, 4.7));
  }
}
