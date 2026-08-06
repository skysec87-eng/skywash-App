package com.skywash.api.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skywash.api.entity.PartnerEntity;

public interface PartnerRepository extends JpaRepository<PartnerEntity, String> {
  List<PartnerEntity> findByActiveTrue();
}
