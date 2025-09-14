package com.ecommerce.bff.infrastructure.external;

import com.ecommerce.bff.application.port.out.BillingServicePort;
import com.ecommerce.bff.infrastructure.external.dto.PaymentDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BillingServiceAdapter implements BillingServicePort {

    private static final Logger logger = LoggerFactory.getLogger(BillingServiceAdapter.class);

    private final WebClient webClient;
    private final String billingServiceBaseUrl;

    public BillingServiceAdapter(WebClient webClient,
                                @Value("${microservices.billing-service.base-url}") String billingServiceBaseUrl) {
        this.webClient = webClient;
        this.billingServiceBaseUrl = billingServiceBaseUrl;
    }

    @Override
    @CircuitBreaker(name = "billing-service", fallbackMethod = "getPaymentByOrderIdFallback")
    @Retry(name = "billing-service")
    public Optional<PaymentDto> getPaymentByOrderId(UUID orderId) {
        logger.debug("Fetching payment for order: {} from {}", orderId, billingServiceBaseUrl);
        
        try {
            var payment = webClient.get()
                .uri(billingServiceBaseUrl + "/api/v1/payments/order/{orderId}", orderId)
                .retrieve()
                .bodyToMono(PaymentDto.class)
                .timeout(Duration.ofSeconds(5))
                .block();
                
            return Optional.ofNullable(payment);
        } catch (Exception ex) {
            logger.error("Error fetching payment for order: {}", orderId, ex);
            return Optional.empty();
        }
    }

    @Override
    @CircuitBreaker(name = "billing-service", fallbackMethod = "getPaymentsByCustomerIdFallback")
    @Retry(name = "billing-service")
    public List<PaymentDto> getPaymentsByCustomerId(UUID customerId) {
        logger.debug("Fetching payments for customer: {} from {}", customerId, billingServiceBaseUrl);
        
        try {
            return webClient.get()
                .uri(billingServiceBaseUrl + "/api/v1/customers/{customerId}/payments", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<PaymentDto>>() {})
                .timeout(Duration.ofSeconds(5))
                .block();
        } catch (Exception ex) {
            logger.error("Error fetching payments for customer: {}", customerId, ex);
            return Collections.emptyList();
        }
    }

    // Fallback methods
    public Optional<PaymentDto> getPaymentByOrderIdFallback(UUID orderId, Exception ex) {
        logger.warn("Using fallback for getPaymentByOrderId. Order: {}", orderId);
        return Optional.empty();
    }

    public List<PaymentDto> getPaymentsByCustomerIdFallback(UUID customerId, Exception ex) {
        logger.warn("Using fallback for getPaymentsByCustomerId. Customer: {}", customerId);
        return Collections.emptyList();
    }
}