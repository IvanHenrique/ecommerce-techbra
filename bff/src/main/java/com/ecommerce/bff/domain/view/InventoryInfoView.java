package com.ecommerce.bff.domain.view;

import java.util.UUID;

public record InventoryInfoView(
    UUID productId,
    String productName,
    Integer quantityReserved,
    String reservationReference,
    String status
) {
}