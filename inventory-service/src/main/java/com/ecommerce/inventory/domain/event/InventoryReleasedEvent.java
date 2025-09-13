package com.ecommerce.inventory.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReleasedEvent(
    @JsonProperty("eventId") UUID eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("aggregateId") UUID aggregateId,
    @JsonProperty("occurredOn") LocalDateTime occurredOn,
    @JsonProperty("version") Integer version,
    @JsonProperty("orderId") UUID orderId,
    @JsonProperty("productId") UUID productId,
    @JsonProperty("productName") String productName,
    @JsonProperty("quantityReleased") Integer quantityReleased,
    @JsonProperty("reason") String reason
) {

    @JsonCreator
    public InventoryReleasedEvent {
        // Compact constructor
    }

    public static InventoryReleasedEvent create(UUID inventoryId, UUID orderId, UUID productId,
                                              String productName, Integer quantityReleased, 
                                              String reason) {
        return new InventoryReleasedEvent(
            UUID.randomUUID(),
            "InventoryReleased",
            inventoryId,
            LocalDateTime.now(),
            1,
            orderId,
            productId,
            productName,
            quantityReleased,
            reason
        );
    }
}