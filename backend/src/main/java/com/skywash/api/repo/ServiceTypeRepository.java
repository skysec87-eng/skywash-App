package com.skywash.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skywash.api.entity.ServiceTypeEntity;

public interface ServiceTypeRepository extends JpaRepository<ServiceTypeEntity, String> {}
