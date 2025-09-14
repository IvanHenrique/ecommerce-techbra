package com.ecommerce.bff.infrastructure.cache;

import com.ecommerce.bff.domain.view.CustomerOrderView;
import com.ecommerce.bff.domain.view.InventoryInfoView;
import com.ecommerce.bff.domain.view.OrderSummaryView;
import com.ecommerce.bff.domain.view.PaymentInfoView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FallbackService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackService.class);

    private final CacheManager cacheManager;

    public FallbackService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public List<OrderSummaryView> getFallbackOrders(UUID customerId) {
        logger.info("Attempting to get cached orders for customer: {}", customerId);
        
        // Try to get from cache first
        Cache cache = cacheManager.getCache("customer-orders");
        if (cache != null) {
            var cachedOrders = cache.get(customerId, List.class);
            if (cachedOrders != null && !cachedOrders.isEmpty()) {
                logger.info("Found {} cached orders for customer: {}", cachedOrders.size(), customerId);
                return (List<OrderSummaryView>) cachedOrders;
            }
        }
        
        // If no cache, return empty with message
        logger.warn("No cached orders available for customer: {}. Returning empty list.", customerId);
        return Collections.emptyList();
    }

    public Optional<CustomerOrderView> getFallbackOrderDetails(UUID orderId) {
        logger.info("Attempting to get cached order details for order: {}", orderId);
        
        Cache cache = cacheManager.getCache("order-details");
        if (cache != null) {
            var cachedOrder = cache.get(orderId, CustomerOrderView.class);
            if (cachedOrder != null) {
                logger.info("Found cached order details for order: {}", orderId);
                return Optional.of(cachedOrder);
            }
        }
        
        logger.warn("No cached order details available for order: {}", orderId);
        return Optional.empty();
    }

    public PaymentInfoView getFallbackPayment(UUID orderId) {
        logger.info("Providing fallback payment info for order: {}", orderId);
        
        // Return a generic "processing" payment status
        return new PaymentInfoView(
            null,
            "PROCESSING",
            BigDecimal.ZERO,
            "USD",
            "PROCESSING",
            "UNKNOWN",
            LocalDateTime.now()
        );
    }

    public List<InventoryInfoView> getFallbackInventory(UUID orderId) {
        logger.info("Providing fallback inventory info for order: {}", orderId);
        
        // Return a generic "processing" inventory status
        return List.of(
            new InventoryInfoView(
                UUID.randomUUID(),
                "Product (Status Unknown)",
                0,
                "PROCESSING",
                "PROCESSING"
            )
        );
    }
}