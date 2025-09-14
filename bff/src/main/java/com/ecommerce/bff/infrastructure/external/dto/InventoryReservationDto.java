package com.ecommerce.bff.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record InventoryReservationDto(
    @JsonProperty("productId") UUID productId,
    @JsonProperty("productName") String productName,
    @JsonProperty("quantityReserved") Integer quantityReserved,
    @JsonProperty("reservationReference") String reservationReference,
    @JsonProperty("status") String status
) {
}