package com.skywash.api.repo;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skywash.api.entity.OtpChallengeEntity;

public interface OtpChallengeRepository extends JpaRepository<OtpChallengeEntity, String> {

  Optional<OtpChallengeEntity> findFirstByEmailAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
      String email, Instant now);

  Optional<OtpChallengeEntity> findByIdAndEmailVerifiedTrueAndConsumedFalseAndExpiresAtAfter(
      String id, Instant now);

  @Modifying(clearAutomatically = true)
  @Query("update OtpChallengeEntity o set o.consumed = true where o.email = :email and o.consumed = false")
  int consumeAllForEmail(@Param("email") String email);
}
