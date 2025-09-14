package com.ecommerce.inventory.adapter.in.web;

import java.util.UUID;

public record ReservationResponseDto(
    UUID productId,
    String productName,
    Integer quantityReserved,
    String reservationReference,
    String status
) {
}