package com.ecommerce.bff.adapter.in.web;

import com.ecommerce.bff.application.port.in.CreateOrderItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull(message = "Customer ID is required")
    UUID customerId,
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    List<CreateOrderItemCommand> items
) {
}