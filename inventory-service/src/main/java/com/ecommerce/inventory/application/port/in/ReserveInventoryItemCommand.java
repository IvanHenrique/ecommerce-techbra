package com.ecommerce.inventory.application.port.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReserveInventoryItemCommand(
    @NotNull(message = "Product ID is required")
    UUID productId,
    
    @NotBlank(message = "Product name is required")
    String productName,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity
) {
}