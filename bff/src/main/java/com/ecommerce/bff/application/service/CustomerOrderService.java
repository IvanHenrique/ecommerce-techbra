package com.ecommerce.bff.application.service;

import com.ecommerce.bff.application.port.in.GetCustomerOrdersQuery;
import com.ecommerce.bff.application.port.in.GetCustomerOrdersUseCase;
import com.ecommerce.bff.application.port.out.OrderServicePort;
import com.ecommerce.bff.domain.view.OrderSummaryView;
import com.ecommerce.bff.infrastructure.cache.FallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerOrderService implements GetCustomerOrdersUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CustomerOrderService.class);

    private final OrderServicePort orderServicePort;
    private final FallbackService fallbackService;

    public CustomerOrderService(OrderServicePort orderServicePort, FallbackService fallbackService) {
        this.orderServicePort = orderServicePort;
        this.fallbackService = fallbackService;
    }

    @Override
    @Cacheable(value = "customer-orders", key = "#query.customerId", unless = "#result.isEmpty()")
    public List<OrderSummaryView> execute(GetCustomerOrdersQuery query) {
        try {
            logger.info("Fetching orders for customer: {}", query.customerId());

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

            logger.info("Successfully fetched {} orders for customer: {}", summaryViews.size(), query.customerId());
            return summaryViews;

        } catch (Exception ex) {
            logger.error("Failed to fetch orders for customer: {}. Using fallback.", query.customerId(), ex);
            return fallbackService.getFallbackOrders(query.customerId());
        }
    }
}