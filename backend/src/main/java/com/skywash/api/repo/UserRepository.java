package com.skywash.api.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skywash.api.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, String> {
  Optional<UserEntity> findByPhone(String phone);
  boolean existsByPhone(String phone);
  Optional<UserEntity> findByEmail(String email);
  boolean existsByEmail(String email);
  Optional<UserEntity> findByGoogleSub(String googleSub);
}
