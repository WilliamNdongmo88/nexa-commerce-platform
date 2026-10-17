package com.microservices.order.repository;

import com.microservices.order.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    List<SagaState> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}