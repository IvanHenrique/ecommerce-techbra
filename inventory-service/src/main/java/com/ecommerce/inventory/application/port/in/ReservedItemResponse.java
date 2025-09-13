package com.ecommerce.inventory.application.port.in;

import java.util.UUID;

public record ReservedItemResponse(
    UUID productId,
    String productName,
    Integer quantityReserved,
    Integer availableQuantity
) {
}