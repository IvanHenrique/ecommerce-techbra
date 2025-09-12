package com.ecommerce.order.application.port.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
    @NotNull(message = "Customer ID is required")
    UUID customerId,
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    List<CreateOrderItemCommand> items
) {
}