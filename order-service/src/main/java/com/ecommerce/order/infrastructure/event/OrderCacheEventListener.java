package com.ecommerce.order.infrastructure.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderCacheEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderCacheEventListener.class);
    
    private final CacheManager cacheManager;

    public OrderCacheEventListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @KafkaListener(topics = "order-events", groupId = "order-cache-invalidation")
    public void handleOrderEvent(String orderEventJson) {
        try {
            logger.info("Received order event for cache invalidation: {}", orderEventJson);
            
            // Parse do evento (simplificado - em produção usar ObjectMapper)
            if (orderEventJson.contains("\"eventType\":\"ORDER_CREATED\"") || 
                orderEventJson.contains("\"eventType\":\"ORDER_UPDATED\"") ||
                orderEventJson.contains("\"eventType\":\"ORDER_CANCELLED\"")) {
                
                // Invalida todos os caches relacionados a pedidos
                invalidateOrderCaches();
                
                // Se conseguir extrair o customerId, invalida cache específico
                String customerId = extractCustomerId(orderEventJson);
                if (customerId != null) {
                    invalidateCustomerOrdersCache(customerId);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing order event for cache invalidation", e);
        }
    }

    private void invalidateOrderCaches() {
        try {
            var ordersCache = cacheManager.getCache("orders");
            if (ordersCache != null) {
                ordersCache.clear();
                logger.info("Invalidated 'orders' cache");
            }
            
            var customerOrdersCache = cacheManager.getCache("customer-orders");
            if (customerOrdersCache != null) {
                customerOrdersCache.clear();
                logger.info("Invalidated 'customer-orders' cache");
            }
        } catch (Exception e) {
            logger.error("Error invalidating order caches", e);
        }
    }

    private void invalidateCustomerOrdersCache(String customerId) {
        try {
            var customerOrdersCache = cacheManager.getCache("customer-orders");
            if (customerOrdersCache != null) {
                customerOrdersCache.evict(UUID.fromString(customerId));
                logger.info("Invalidated customer-orders cache for customer: {}", customerId);
            }
        } catch (Exception e) {
            logger.error("Error invalidating customer orders cache for customer: {}", customerId, e);
        }
    }

    private String extractCustomerId(String orderEventJson) {
        try {
            // Extração simples do customerId do JSON
            // Em produção, usar ObjectMapper para parsing adequado
            int customerIdIndex = orderEventJson.indexOf("\"customerId\":\"");
            if (customerIdIndex != -1) {
                int start = customerIdIndex + 14; // tamanho de "customerId":"
                int end = orderEventJson.indexOf("\"", start);
                if (end != -1) {
                    return orderEventJson.substring(start, end);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract customerId from event: {}", orderEventJson);
        }
        return null;
    }
}