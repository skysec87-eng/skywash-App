package com.skywash.api.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skywash.api.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
  List<OrderEntity> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
  List<OrderEntity> findAllByOrderByCreatedAtDesc();
  List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
  List<OrderEntity> findByStatusNotIn(List<String> statuses);
  Optional<OrderEntity> findByPaymentReference(String paymentReference);
}
