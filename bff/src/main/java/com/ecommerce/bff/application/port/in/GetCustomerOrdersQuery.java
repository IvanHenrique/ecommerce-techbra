package com.ecommerce.bff.application.port.in;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetCustomerOrdersQuery(
    @NotNull(message = "Customer ID is required")
    UUID customerId
) {
}