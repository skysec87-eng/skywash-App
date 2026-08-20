package com.skywash.api.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skywash.api.entity.PartnerNotificationEntity;

public interface PartnerNotificationRepository extends JpaRepository<PartnerNotificationEntity, String> {
  List<PartnerNotificationEntity> findByOrderIdOrderByCreatedAtDesc(String orderId);
  List<PartnerNotificationEntity> findByPartnerIdAndOrderIdIn(String partnerId, List<String> orderIds);
}
