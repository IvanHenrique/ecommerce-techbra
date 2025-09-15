package com.ecommerce.bff.adapter.in.web;

import com.ecommerce.bff.application.port.in.*;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Customer Orders", description = "Customer order aggregation and details")
public class CustomerOrderController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderController.class);

    private final GetCustomerOrdersUseCase getCustomerOrdersUseCase;
    private final GetOrderDetailsUseCase getOrderDetailsUseCase;
    private final CreateOrderUseCase createOrderUseCase;

    public CustomerOrderController(GetCustomerOrdersUseCase getCustomerOrdersUseCase,
                                   GetOrderDetailsUseCase getOrderDetailsUseCase,
                                   CreateOrderUseCase createOrderUseCase) {
        this.getCustomerOrdersUseCase = getCustomerOrdersUseCase;
        this.getOrderDetailsUseCase = getOrderDetailsUseCase;
        this.createOrderUseCase = createOrderUseCase;
    }

    @GetMapping("/customers/{customerId}/orders")
    @Operation(summary = "Get customer orders", description = "Retrieves all orders for a specific customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid customer ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getCustomerOrders(@PathVariable UUID customerId) {
        logger.info("Received request to get orders for customer: {}", customerId);

        try {
            var query = new GetCustomerOrdersQuery(customerId);
            var orders = getCustomerOrdersUseCase.execute(query);

            logger.info("Successfully retrieved {} orders for customer: {}", orders.size(), customerId);
            return ResponseEntity.ok(orders);

        } catch (Exception ex) {
            logger.error("Failed to get orders for customer: {}", customerId, ex);
            throw new BusinessException("CUSTOMER_ORDERS_FETCH_FAILED", "Failed to retrieve customer orders");
        }
    }

    @GetMapping("/orders/{orderId}/details")
    @Operation(summary = "Get order details", description = "Retrieves complete details for a specific order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getOrderDetails(@PathVariable UUID orderId) {
        logger.info("Received request to get details for order: {}", orderId);

        try {
            var query = new GetOrderDetailsQuery(orderId);
            var orderDetails = getOrderDetailsUseCase.execute(query);

            if (orderDetails.isEmpty()) {
                logger.warn("Order not found: {}", orderId);
                throw new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderId);
            }

            logger.info("Successfully retrieved details for order: {}", orderId);
            return ResponseEntity.ok(orderDetails.get());

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Failed to get details for order: {}", orderId, ex);
            throw new BusinessException("ORDER_DETAILS_FETCH_FAILED", "Failed to retrieve order details");
        }
    }

    @PostMapping("/orders")
    @Operation(summary = "Create order via BFF", description = "Creates a new order through the BFF")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Received request to create order via BFF for customer: {}", request.customerId());

        try {
            var command = new CreateOrderCommand(request.customerId(), request.items());
            var result = createOrderUseCase.execute(command);

            if (result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(result.getValue());
            } else {
                throw new BusinessException(result.getErrorCode(), result.getErrorMessage());
            }

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Failed to create order via BFF", ex);
            throw new BusinessException("ORDER_CREATION_FAILED", "Failed to create order");
        }
    }

}