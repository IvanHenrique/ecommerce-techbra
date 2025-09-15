package com.ecommerce.bff.application.port.in;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemCommand(
    @NotNull(message = "Product ID is required")
    UUID productId,
    
    @NotBlank(message = "Product name is required")
    String productName,
    
    @Positive(message = "Quantity must be positive")
    Integer quantity,
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    BigDecimal unitPrice,
    
    @NotBlank(message = "Currency is required")
    String currency
) {
}