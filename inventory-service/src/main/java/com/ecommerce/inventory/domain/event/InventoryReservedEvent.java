package com.ecommerce.inventory.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservedEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("aggregateId") UUID aggregateId,
    @JsonProperty("occurredOn") LocalDateTime occurredOn,
    @JsonProperty("version") Integer version,
    @JsonProperty("orderId") UUID orderId,
    @JsonProperty("productId") UUID productId,
    @JsonProperty("productName") String productName,
    @JsonProperty("quantityReserved") Integer quantityReserved,
    @JsonProperty("reservationReference") String reservationReference
) {

    @JsonCreator
    public InventoryReservedEvent {
        // Compact constructor
    }

    public static InventoryReservedEvent create(UUID inventoryId, UUID orderId, UUID productId,
                                              String productName, Integer quantityReserved, 
                                              String reservationReference) {
        return new InventoryReservedEvent(
            UUID.randomUUID(),
            "InventoryReserved",
            inventoryId,
            LocalDateTime.now(),
            1,
            orderId,
            productId,
            productName,
            quantityReserved,
            reservationReference
        );
    }
}