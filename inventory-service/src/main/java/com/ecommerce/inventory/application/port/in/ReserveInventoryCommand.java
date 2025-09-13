package com.ecommerce.inventory.application.port.in;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryCommand(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotNull(message = "Items are required")
        List<ReserveInventoryItemCommand> items,

        @NotNull(message = "Idempotency key is required")
        String idempotencyKey
) {
}