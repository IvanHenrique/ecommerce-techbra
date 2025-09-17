package com.ecommerce.bff.application.service;

import com.ecommerce.bff.application.port.in.GetCustomerOrdersQuery;
import com.ecommerce.bff.application.port.in.GetCustomerOrdersUseCase;
import com.ecommerce.bff.application.port.out.OrderServicePort;
import com.ecommerce.bff.domain.view.OrderSummaryView;
import com.ecommerce.bff.infrastructure.cache.FallbackService;
import com.ecommerce.bff.infrastructure.cache.ManualCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerOrderService implements GetCustomerOrdersUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderService.class);

    private static final TypeReference<List<OrderSummaryView>> ORDER_SUMMARY_LIST_TYPE =
            new TypeReference<List<OrderSummaryView>>() {};

    private final OrderServicePort orderServicePort;
    private final FallbackService fallbackService;
    private final ManualCacheService manualCacheService;

    public CustomerOrderService(
            OrderServicePort orderServicePort,
            FallbackService fallbackService,
            ManualCacheService manualCacheService) {
        this.orderServicePort = orderServicePort;
        this.fallbackService = fallbackService;
        this.manualCacheService = manualCacheService;
    }

    @Override
    @Cacheable(value = "customer-orders", key = "#query.customerId", unless = "#result.isEmpty()")  // L1 Cache (Caffeine)
    public List<OrderSummaryView> execute(GetCustomerOrdersQuery query) {
        String l2CacheKey = "customer-orders-l2::" + query.customerId();

        try {
            // 1. Tentar L2 cache (Redis) primeiro
            Optional<List<OrderSummaryView>> cachedOrders =
                    manualCacheService.get(l2CacheKey, ORDER_SUMMARY_LIST_TYPE);

            if (cachedOrders.isPresent()) {
                logger.info("L2 cache hit for customer orders: {}", query.customerId());
                return cachedOrders.get();
            }

            // 2. L2 cache miss - buscar do database
            logger.info("L2 cache miss - fetching orders for customer from database: {}", query.customerId());
            var orders = orderServicePort.getOrdersByCustomerId(query.customerId());

            var summaryViews = orders.stream()
                    .map(order -> new OrderSummaryView(
                            order.orderId(),
                            order.orderNumber(),
                            order.totalAmount(),
                            order.currency(),
                            order.status(),
                            order.orderDate()
                    ))
                    .toList();

            // 3. Armazenar no L2 cache (Redis - 1 hora)
            if (!summaryViews.isEmpty()) {
                manualCacheService.put(l2CacheKey, summaryViews, Duration.ofHours(1));
            }

            logger.info("Successfully fetched {} orders for customer: {}", summaryViews.size(), query.customerId());
            return summaryViews;

        } catch (Exception ex) {
            logger.error("Failed to fetch orders for customer: {}. Using fallback.", query.customerId(), ex);
            return fallbackService.getFallbackOrders(query.customerId());
        }
    }
}