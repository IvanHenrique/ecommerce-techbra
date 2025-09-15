package com.ecommerce.bff.infrastructure.external;

import com.ecommerce.bff.application.port.in.CreateOrderCommand;
import com.ecommerce.bff.application.port.out.OrderServicePort;
import com.ecommerce.bff.infrastructure.external.dto.OrderDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Component
public class OrderServiceAdapter implements OrderServicePort {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceAdapter.class);

    private final WebClient webClient;
    private final String orderServiceBaseUrl;

    public OrderServiceAdapter(WebClient webClient, 
                              @Value("${microservices.order-service.base-url}") String orderServiceBaseUrl) {
        this.webClient = webClient;
        this.orderServiceBaseUrl = orderServiceBaseUrl;
    }

    @Override
    @CircuitBreaker(name = "order-service", fallbackMethod = "getOrdersByCustomerIdFallback")
    @Retry(name = "order-service")
    public List<OrderDto> getOrdersByCustomerId(UUID customerId) {
        logger.debug("Fetching orders for customer: {} from {}", customerId, orderServiceBaseUrl);
        
        try {
            return webClient.get()
                .uri(orderServiceBaseUrl + "/api/v1/orders/customers/{customerId}/orders", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<OrderDto>>() {})
                .timeout(Duration.ofSeconds(30))
                .block();
        } catch (Exception ex) {
            logger.error("Error fetching orders for customer: {}", customerId, ex);
            throw ex;
        }
    }

    @Override
    @CircuitBreaker(name = "order-service", fallbackMethod = "getOrderByIdFallback")
    @Retry(name = "order-service")
    public Optional<OrderDto> getOrderById(UUID orderId) {
        logger.debug("Fetching order: {} from {}", orderId, orderServiceBaseUrl);
        
        try {
            var order = webClient.get()
                .uri(orderServiceBaseUrl + "/api/v1/orders/{orderId}", orderId)
                .retrieve()
                .bodyToMono(OrderDto.class)
                .timeout(Duration.ofSeconds(30))
                .block();
                
            return Optional.ofNullable(order);
        } catch (Exception ex) {
            logger.error("Error fetching order: {}", orderId, ex);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "order-service", fallbackMethod = "createOrderFallback")
    @Retry(name = "order-service")
    public Optional<OrderDto> createOrder(CreateOrderCommand command) {
        logger.debug("Creating order via order service: {}", orderServiceBaseUrl);

        try {
            var requestBody = Map.of(
                    "customerId", command.customerId(),
                    "items", command.items().stream()
                            .map(item -> Map.of(
                                    "productId", item.productId(),
                                    "productName", item.productName(),
                                    "quantity", item.quantity(),
                                    "unitPrice", item.unitPrice(),
                                    "currency", item.currency()
                            ))
                            .toList()
            );

            var order = webClient.post()
                    .uri(orderServiceBaseUrl + "/api/v1/orders")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OrderDto.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return Optional.ofNullable(order);
        } catch (Exception ex) {
            logger.error("Error creating order", ex);
            return Optional.empty();
        }
    }

    // Fallback methods
    public List<OrderDto> getOrdersByCustomerIdFallback(UUID customerId, Exception ex) {
        logger.warn("Using fallback for getOrdersByCustomerId. Customer: {}", customerId);
        return Collections.emptyList();
    }

    public Optional<OrderDto> getOrderByIdFallback(UUID orderId, Exception ex) {
        logger.warn("Using fallback for getOrderById. Order: {}", orderId);
        return Optional.empty();
    }

    public Optional<OrderDto> createOrderFallback(CreateOrderCommand command, Exception ex) {
        logger.warn("Using fallback for createOrder");
        return Optional.empty();
    }
}