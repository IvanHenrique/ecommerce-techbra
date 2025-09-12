package com.ecommerce.billing.application.port.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentCommand(
    @NotNull(message = "Order ID is required")
    UUID orderId,
    
    @NotNull(message = "Customer ID is required")
    UUID customerId,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    @NotBlank(message = "Currency is required")
    String currency,
    
    @NotBlank(message = "Payment method is required")
    String paymentMethod,
    
    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey
) {
}