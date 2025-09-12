package com.ecommerce.billing.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
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
    @JsonProperty("paymentMethod") String paymentMethod
) {

    @JsonCreator
    public PaymentCompletedEvent {
        // Compact constructor
    }

    public static PaymentCompletedEvent create(UUID paymentId, UUID orderId, UUID customerId,
                                             String paymentReference, BigDecimal amount, 
                                             String currency, String paymentMethod) {
        return new PaymentCompletedEvent(
            UUID.randomUUID(),
            "PaymentCompleted",
            paymentId,
            LocalDateTime.now(),
            1,
            orderId,
            customerId,
            paymentReference,
            amount,
            currency,
            paymentMethod
        );
    }
}