package com.ecommerce.order.adapter.in.web;

import com.ecommerce.order.application.port.in.CreateOrderCommand;
import com.ecommerce.order.application.port.in.CreateOrderUseCase;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Received request to create order for customer: {}", request.customerId());

        var command = new CreateOrderCommand(request.customerId(), request.items());
        var result = createOrderUseCase.execute(command);

        if (result.isSuccess()) {
            var response = result.getValue();
            logger.info("Order created successfully with ID: {}", response.orderId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            logger.warn("Failed to create order: {}", result.getErrorMessage());
            throw new BusinessException(result.getErrorCode(), result.getErrorMessage());
        }
    }
}