package com.ecommerce.order.adapter.in.web;

import com.ecommerce.order.application.port.in.CreateOrderItemCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new order")
public record CreateOrderRequest(
    @NotNull(message = "Customer ID is required")
    @Schema(description = "Customer ID", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID customerId,
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    @Schema(description = "List of order items")
    List<CreateOrderItemCommand> items
) {
}