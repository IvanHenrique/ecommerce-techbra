package com.ecommerce.billing.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("aggregateId") UUID aggregateId,
    @JsonProperty("occurredOn") LocalDateTime occurredOn,
    @JsonProperty("version") Integer version,
    @JsonProperty("orderId") UUID orderId,
    @JsonProperty("customerId") UUID customerId,
    @JsonProperty("paymentReference") String paymentReference,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency,
    @JsonProperty("failureReason") String failureReason
) {

    @JsonCreator
    public PaymentFailedEvent {
        // Compact constructor
    }

    public static PaymentFailedEvent create(UUID paymentId, UUID orderId, UUID customerId,
                                          String paymentReference, BigDecimal amount, 
                                          String currency, String failureReason) {
        return new PaymentFailedEvent(
            UUID.randomUUID(),
            "PaymentFailed",
            paymentId,
            LocalDateTime.now(),
            1,
            orderId,
            customerId,
            paymentReference,
            amount,
            currency,
            failureReason
        );
    }
}