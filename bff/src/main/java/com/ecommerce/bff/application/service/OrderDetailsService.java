package com.ecommerce.bff.application.service;

import com.ecommerce.bff.application.port.in.GetOrderDetailsQuery;
import com.ecommerce.bff.application.port.in.GetOrderDetailsUseCase;
import com.ecommerce.bff.application.port.out.BillingServicePort;
import com.ecommerce.bff.application.port.out.InventoryServicePort;
import com.ecommerce.bff.application.port.out.OrderServicePort;
import com.ecommerce.bff.domain.view.CustomerOrderView;
import com.ecommerce.bff.domain.view.InventoryInfoView;
import com.ecommerce.bff.domain.view.PaymentInfoView;
import com.ecommerce.bff.infrastructure.cache.FallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class OrderDetailsService implements GetOrderDetailsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(OrderDetailsService.class);

    private final OrderServicePort orderServicePort;
    private final BillingServicePort billingServicePort;
    private final InventoryServicePort inventoryServicePort;
    private final FallbackService fallbackService;

    public OrderDetailsService(OrderServicePort orderServicePort,
                             BillingServicePort billingServicePort,
                             InventoryServicePort inventoryServicePort,
                             FallbackService fallbackService) {
        this.orderServicePort = orderServicePort;
        this.billingServicePort = billingServicePort;
        this.inventoryServicePort = inventoryServicePort;
        this.fallbackService = fallbackService;
    }

    @Override
    @Cacheable(value = "order-details", key = "#query.orderId")
    public Optional<CustomerOrderView> execute(GetOrderDetailsQuery query) {
        try {
            logger.info("Fetching order details for order: {}", query.orderId());
            
            // Fetch order data
            var orderOpt = orderServicePort.getOrderById(query.orderId());
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found: {}", query.orderId());
                return Optional.empty();
            }
            
            var order = orderOpt.get();
            
            // Fetch payment data (with fallback)
            PaymentInfoView paymentInfo = null;
            try {
                var paymentOpt = billingServicePort.getPaymentByOrderId(query.orderId());
                if (paymentOpt.isPresent()) {
                    var payment = paymentOpt.get();
                    paymentInfo = new PaymentInfoView(
                        payment.paymentId(),
                        payment.paymentReference(),
                        payment.amount(),
                        payment.currency(),
                        payment.status(),
                        payment.paymentMethod(),
                        payment.processedAt()
                    );
                }
            } catch (Exception ex) {
                logger.warn("Failed to fetch payment for order: {}. Using fallback.", query.orderId(), ex);
                paymentInfo = fallbackService.getFallbackPayment(query.orderId());
            }
            
            // Fetch inventory data (with fallback)
            var inventoryInfo = Collections.<InventoryInfoView>emptyList();
            try {
                var reservations = inventoryServicePort.getReservationsByOrderId(query.orderId());
                inventoryInfo = reservations.stream()
                    .map(res -> new InventoryInfoView(
                        res.productId(),
                        res.productName(),
                        res.quantityReserved(),
                        res.reservationReference(),
                        res.status()
                    ))
                    .toList();
            } catch (Exception ex) {
                logger.warn("Failed to fetch inventory for order: {}. Using fallback.", query.orderId(), ex);
                inventoryInfo = fallbackService.getFallbackInventory(query.orderId());
            }
            
            var customerOrderView = CustomerOrderView.create(
                order.orderId(),
                order.orderNumber(),
                order.customerId(),
                order.totalAmount(),
                order.currency(),
                order.status(),
                order.orderDate(),
                paymentInfo,
                inventoryInfo
            );
            
            logger.info("Successfully aggregated order details for order: {}", query.orderId());
            return Optional.of(customerOrderView);
            
        } catch (Exception ex) {
            logger.error("Failed to fetch order details for order: {}", query.orderId(), ex);
            return fallbackService.getFallbackOrderDetails(query.orderId());
        }
    }
}