package com.ecommerce.inventory.adapter.in.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEventDto(
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
}