package com.ecommerce.billing.adapter.out.persistence;

import com.ecommerce.billing.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT p FROM Payment p WHERE p.customerId = :customerId ORDER BY p.createdAt DESC")
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") UUID customerId);
    
    boolean existsByOrderId(UUID orderId);
}