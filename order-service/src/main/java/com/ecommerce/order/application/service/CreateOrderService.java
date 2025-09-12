package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.in.CreateOrderCommand;
import com.ecommerce.order.application.port.in.CreateOrderResponse;
import com.ecommerce.order.application.port.in.CreateOrderUseCase;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.shared.domain.common.Result;
import com.ecommerce.shared.domain.valueobject.Money;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateOrderService.class);

    private final OrderRepositoryPort orderRepository;
    private final OrderEventPublisherPort eventPublisher;

    public CreateOrderService(OrderRepositoryPort orderRepository, 
                             OrderEventPublisherPort eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result<CreateOrderResponse> execute(CreateOrderCommand command) {
        try {
            logger.info("Creating order for customer: {}", command.customerId());

            // Generate unique order number
            String orderNumber = generateOrderNumber();
            
            // Validate order number uniqueness
            if (orderRepository.existsByOrderNumber(orderNumber)) {
                return Result.failure("ORDER_NUMBER_EXISTS", "Order number already exists");
            }

            // Create order with initial amount (will be recalculated)
            var order = new Order(orderNumber, command.customerId(), Money.zero("USD"));

            // Add items to order
            for (var itemCommand : command.items()) {
                var unitPrice = Money.of(itemCommand.unitPrice(), itemCommand.currency());
                var orderItem = new OrderItem(
                    itemCommand.productId(),
                    itemCommand.productName(),
                    itemCommand.quantity(),
                    unitPrice
                );
                order.addItem(orderItem);
            }

            // Save order
            var savedOrder = orderRepository.save(order);
            logger.info("Order created with ID: {} and number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

            // Publish domain event
            var event = OrderCreatedEvent.create(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount().getAmount(),
                savedOrder.getTotalAmount().getCurrencyCode()
            );
            
            eventPublisher.publishOrderCreated(event);
            logger.info("OrderCreated event published for order: {}", savedOrder.getOrderNumber());

            // Create response
            var response = new CreateOrderResponse(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount().getAmount(),
                savedOrder.getTotalAmount().getCurrencyCode(),
                savedOrder.getStatus().name(),
                savedOrder.getOrderDate()
            );

            return Result.success(response);

        } catch (BusinessException ex) {
            logger.error("Business error creating order: {}", ex.getMessage());
            return Result.failure(ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error creating order", ex);
            return Result.failure("ORDER_CREATION_FAILED", "Failed to create order");
        }
    }

    private String generateOrderNumber() {
        // Simple implementation - in production, use a more sophisticated approach
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}