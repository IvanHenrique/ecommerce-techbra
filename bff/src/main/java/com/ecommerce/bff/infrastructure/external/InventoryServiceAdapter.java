package com.ecommerce.bff.infrastructure.external;

import com.ecommerce.bff.application.port.out.InventoryServicePort;
import com.ecommerce.bff.infrastructure.external.dto.InventoryReservationDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class InventoryServiceAdapter implements InventoryServicePort {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceAdapter.class);

    private final WebClient webClient;
    private final String inventoryServiceBaseUrl;

    public InventoryServiceAdapter(WebClient webClient,
                                  @Value("${microservices.inventory-service.base-url}") String inventoryServiceBaseUrl) {
        this.webClient = webClient;
        this.inventoryServiceBaseUrl = inventoryServiceBaseUrl;
    }

    @Override
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "getReservationsByOrderIdFallback")
    @Retry(name = "inventory-service")
//    @TimeLimiter(name = "inventory-service")
    public List<InventoryReservationDto> getReservationsByOrderId(UUID orderId) {
        logger.debug("Fetching reservations for order: {} from {}", orderId, inventoryServiceBaseUrl);
        
        try {
            return webClient.get()
                .uri(inventoryServiceBaseUrl + "/api/v1/reservations/order/{orderId}", orderId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<InventoryReservationDto>>() {})
                .timeout(Duration.ofSeconds(5))
                .block();
        } catch (Exception ex) {
            logger.error("Error fetching reservations for order: {}", orderId, ex);
            return Collections.emptyList();
        }
    }

    // Fallback method
    public List<InventoryReservationDto> getReservationsByOrderIdFallback(UUID orderId, Exception ex) {
        logger.warn("Using fallback for getReservationsByOrderId. Order: {}", orderId);
        return Collections.emptyList();
    }
}