package com.ecommerce.billing.adapter.out.persistence;

import com.ecommerce.billing.application.port.out.PaymentRepositoryPort;
import com.ecommerce.billing.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return jpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Payment> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }
}