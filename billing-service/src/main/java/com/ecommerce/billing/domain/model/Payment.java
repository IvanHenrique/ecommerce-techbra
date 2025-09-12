package com.ecommerce.billing.domain.model;

import com.ecommerce.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "payment_reference", unique = true, nullable = false)
    private String paymentReference;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    protected Payment() {
        // JPA Constructor
    }

    public Payment(UUID orderId, UUID customerId, String paymentReference,
                   BigDecimal amount, String currency, PaymentMethod paymentMethod,
                   String idempotencyKey) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.paymentReference = paymentReference;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
    }

    public void markAsCompleted() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be completed");
        }
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        if (this.status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Completed payments cannot be marked as failed");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}