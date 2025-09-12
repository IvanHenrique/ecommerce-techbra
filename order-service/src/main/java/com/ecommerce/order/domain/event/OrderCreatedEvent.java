package com.ecommerce.order.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
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

    @JsonCreator
    public OrderCreatedEvent {
        // Compact constructor with validation if needed
    }

    public static OrderCreatedEvent create(UUID orderId, String orderNumber, UUID customerId,
                                           BigDecimal totalAmount, String currency) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                "OrderCreated",
                orderId,
                LocalDateTime.now(),
                1,
                orderNumber,
                customerId,
                totalAmount,
                currency
        );
    }
}