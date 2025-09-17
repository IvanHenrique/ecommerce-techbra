package com.ecommerce.bff.infrastructure.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationEventListener.class);

    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;

    public CacheInvalidationEventListener(CacheManager caffeineCacheManager, CacheManager redisCacheManager) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
    }

    @KafkaListener(topics = "order-events", groupId = "bff-cache-invalidation")
    public void handleOrderEvent(String message) {
        logger.info("Received order event for cache invalidation: {}", message);
        
        try {
            // Parse event to extract order and customer IDs
            var eventData = parseOrderEvent(message);
            
            if (eventData != null) {
                // Invalidate L1 caches
                invalidateCache(caffeineCacheManager, "customer-orders", eventData.customerId());
                invalidateCache(caffeineCacheManager, "order-details", eventData.orderId());
                
                // Invalidate L2 caches
                invalidateCache(redisCacheManager, "customer-orders-l2", eventData.customerId());
                invalidateCache(redisCacheManager, "order-details-l2", eventData.orderId());
                invalidateCache(redisCacheManager, "aggregated-views", "customer-dashboard-" + eventData.customerId());
                invalidateCache(redisCacheManager, "aggregated-views", "order-full-view-" + eventData.orderId());
                
                logger.info("Successfully invalidated caches for order: {} and customer: {}", 
                           eventData.orderId(), eventData.customerId());
            }
        } catch (Exception ex) {
            logger.error("Failed to process order event for cache invalidation: {}", message, ex);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "bff-cache-invalidation")
    public void handlePaymentEvent(String message) {
        logger.info("Received payment event for cache invalidation: {}", message);
        
        try {
            var eventData = parsePaymentEvent(message);
            
            if (eventData != null) {
                // Invalidate L1 caches
                invalidateCache(caffeineCacheManager, "order-details", eventData.orderId());
                
                // Invalidate L2 caches
                invalidateCache(redisCacheManager, "order-details-l2", eventData.orderId());
                invalidateCache(redisCacheManager, "aggregated-views", "customer-dashboard-" + eventData.customerId());
                invalidateCache(redisCacheManager, "aggregated-views", "order-full-view-" + eventData.orderId());
                
                logger.info("Successfully invalidated caches for payment event - order: {} and customer: {}", 
                           eventData.orderId(), eventData.customerId());
            }
        } catch (Exception ex) {
            logger.error("Failed to process payment event for cache invalidation: {}", message, ex);
        }
    }

    @KafkaListener(topics = "inventory-events", groupId = "bff-cache-invalidation")
    public void handleInventoryEvent(String message) {
        logger.info("Received inventory event for cache invalidation: {}", message);
        
        try {
            var eventData = parseInventoryEvent(message);
            
            if (eventData != null) {
                // Invalidate L1 caches
                invalidateCache(caffeineCacheManager, "order-details", eventData.orderId());
                
                // Invalidate L2 caches
                invalidateCache(redisCacheManager, "order-details-l2", eventData.orderId());
                invalidateCache(redisCacheManager, "aggregated-views", "order-full-view-" + eventData.orderId());
                
                logger.info("Successfully invalidated caches for inventory event - order: {}", eventData.orderId());
            }
        } catch (Exception ex) {
            logger.error("Failed to process inventory event for cache invalidation: {}", message, ex);
        }
    }

    private void invalidateCache(CacheManager cacheManager, String cacheName, String key) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("Evicted cache entry - cache: {}, key: {}", cacheName, key);
            }
        } catch (Exception ex) {
            logger.warn("Failed to evict cache entry - cache: {}, key: {}", cacheName, key, ex);
        }
    }

    private OrderEventData parseOrderEvent(String message) {
        try {
            // Simple parsing - in production, use proper JSON parsing
            if (message.contains("orderId") && message.contains("customerId")) {
                // Extract IDs from message (simplified implementation)
                String orderId = extractValue(message, "orderId");
                String customerId = extractValue(message, "customerId");
                return new OrderEventData(orderId, customerId);
            }
        } catch (Exception ex) {
            logger.warn("Failed to parse order event: {}", message, ex);
        }
        return null;
    }

    private PaymentEventData parsePaymentEvent(String message) {
        try {
            if (message.contains("orderId") && message.contains("customerId")) {
                String orderId = extractValue(message, "orderId");
                String customerId = extractValue(message, "customerId");
                return new PaymentEventData(orderId, customerId);
            }
        } catch (Exception ex) {
            logger.warn("Failed to parse payment event: {}", message, ex);
        }
        return null;
    }

    private InventoryEventData parseInventoryEvent(String message) {
        try {
            if (message.contains("orderId")) {
                String orderId = extractValue(message, "orderId");
                return new InventoryEventData(orderId);
            }
        } catch (Exception ex) {
            logger.warn("Failed to parse inventory event: {}", message, ex);
        }
        return null;
    }

    private String extractValue(String message, String key) {
        // Simplified extraction - in production, use proper JSON parsing
        int startIndex = message.indexOf(key + "\":\"") + key.length() + 3;
        int endIndex = message.indexOf("\"", startIndex);
        return message.substring(startIndex, endIndex);
    }

    // Event data records
    private record OrderEventData(String orderId, String customerId) {}
    private record PaymentEventData(String orderId, String customerId) {}
    private record InventoryEventData(String orderId) {}
}