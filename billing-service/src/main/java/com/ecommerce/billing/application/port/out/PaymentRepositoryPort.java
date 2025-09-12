package com.ecommerce.billing.application.port.out;

import com.ecommerce.billing.domain.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(UUID id);
    
    Optional<Payment> findByOrderId(UUID orderId);
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    List<Payment> findByCustomerId(UUID customerId);
    
    boolean existsByOrderId(UUID orderId);
}