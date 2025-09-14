package com.ecommerce.inventory.application.port.in;

import java.util.UUID;

public record GetReservationResponse(
    UUID productId,
    String productName,
    Integer quantityReserved,
    String reservationReference,
    String status
) {
}