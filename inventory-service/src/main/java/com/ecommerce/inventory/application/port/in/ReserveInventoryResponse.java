package com.ecommerce.inventory.application.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReserveInventoryResponse(
    UUID orderId,
    String reservationReference,
    List<ReservedItemResponse> reservedItems,
    String status,
    LocalDateTime reservedAt
) {
}