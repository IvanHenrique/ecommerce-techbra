package com.ecommerce.bff.application.port.in;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetOrderDetailsQuery(
    @NotNull(message = "Order ID is required")
    UUID orderId
) {
}