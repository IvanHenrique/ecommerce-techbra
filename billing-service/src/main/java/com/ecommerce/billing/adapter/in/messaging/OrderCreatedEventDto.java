package com.ecommerce.billing.adapter.in.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEventDto(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("aggregateId") UUID aggregateId,
    @JsonProperty("occurredOn") LocalDateTime occurredOn,
    @JsonProperty("version") Integer version,
    @JsonProperty("orderNumber") String orderNumber,
    @JsonProperty("customerId") UUID customerId,
    @JsonProperty("totalAmount") BigDecimal totalAmount,
    @JsonProperty("currency") String currency
) {
}