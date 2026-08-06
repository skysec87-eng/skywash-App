package com.skywash.api.repo;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skywash.api.entity.AuthSessionEntity;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, String> {
  Optional<AuthSessionEntity> findByTokenAndExpiresAtAfter(String token, Instant now);
  void deleteByExpiresAtBefore(Instant now);
  void deleteByUserId(String userId);
}
