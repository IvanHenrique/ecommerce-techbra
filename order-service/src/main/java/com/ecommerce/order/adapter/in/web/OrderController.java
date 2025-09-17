package com.ecommerce.order.adapter.in.web;

import com.ecommerce.order.application.port.in.*;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrdersByCustomerUseCase getOrdersByCustomerUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           GetOrdersByCustomerUseCase getOrdersByCustomerUseCase,
                           GetOrderByIdUseCase getOrderByIdUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrdersByCustomerUseCase = getOrdersByCustomerUseCase;
        this.getOrderByIdUseCase = getOrderByIdUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Bulkhead(name = "order-creation", type = Bulkhead.Type.SEMAPHORE)
    @RateLimiter(name = "order-creation")
    @CircuitBreaker(name = "order-creation")
    @Retry(name = "order-creation")
    @CacheEvict(value = {"orders", "customer-orders"}, allEntries = true)
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
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

    @GetMapping("/customers/{customerId}/orders")
    @Operation(summary = "Get orders by customer", description = "Retrieves all orders for a specific customer")
    @Bulkhead(name = "order-query", type = Bulkhead.Type.SEMAPHORE)
    @RateLimiter(name = "order-query")
    @CircuitBreaker(name = "order-query")
    @Retry(name = "order-query")
    @Cacheable(value = "customer-orders", key = "#customerId")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByCustomer(@PathVariable UUID customerId) {
        logger.info("Received request to get orders for customer: {}", customerId);

        var query = new GetOrdersByCustomerQuery(customerId);
        var orders = getOrdersByCustomerUseCase.execute(query);

        var response = orders.stream()
                .map(order -> new OrderResponseDto(
                        order.orderId(),
                        order.orderNumber(),
                        order.customerId(),
                        order.totalAmount(),
                        order.currency(),
                        order.status(),
                        order.orderDate()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves a specific order by its ID")
    @ApiResponse(responseCode = "200", description = "Order retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @Bulkhead(name = "order-query", type = Bulkhead.Type.SEMAPHORE)
    @RateLimiter(name = "order-query")
    @CircuitBreaker(name = "order-query")
    @Retry(name = "order-query")
    @Cacheable(value = "orders", key = "#orderId")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable UUID orderId) {
        logger.info("Received request to get order: {}", orderId);

        var query = new GetOrderByIdQuery(orderId);
        var orderOpt = getOrderByIdUseCase.execute(query);

        if (orderOpt.isEmpty()) {
            throw new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderId);
        }

        var order = orderOpt.get();
        var response = new OrderResponseDto(
                order.orderId(),
                order.orderNumber(),
                order.customerId(),
                order.totalAmount(),
                order.currency(),
                order.status(),
                order.orderDate()
        );

        return ResponseEntity.ok(response);
    }

}