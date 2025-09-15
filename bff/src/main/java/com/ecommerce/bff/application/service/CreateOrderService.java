package com.ecommerce.bff.application.service;

import com.ecommerce.bff.application.port.in.CreateOrderCommand;
import com.ecommerce.bff.application.port.in.CreateOrderResponse;
import com.ecommerce.bff.application.port.in.CreateOrderUseCase;
import com.ecommerce.bff.application.port.out.OrderServicePort;
import com.ecommerce.shared.domain.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateOrderService.class);

    private final OrderServicePort orderServicePort;

    public CreateOrderService(OrderServicePort orderServicePort) {
        this.orderServicePort = orderServicePort;
    }

    @Override
    public Result<CreateOrderResponse> execute(CreateOrderCommand command) {
        logger.info("Creating order via BFF for customer: {}", command.customerId());
        
        try {
            // Aqui o BFF chama o Order Service
            var orderResult = orderServicePort.createOrder(command);
            
            if (orderResult.isPresent()) {
                var order = orderResult.get();
                var response = new CreateOrderResponse(
                    order.orderId(),
                    order.orderNumber(),
                    order.customerId(),
                    order.totalAmount(),
                    order.currency(),
                    order.status(),
                    order.orderDate()
                );
                
                logger.info("Order created successfully via BFF: {}", order.orderId());
                return Result.success(response);
            } else {
                return Result.failure("ORDER_CREATION_FAILED", "Failed to create order");
            }
            
        } catch (Exception ex) {
            logger.error("Failed to create order via BFF", ex);
            return Result.failure("ORDER_CREATION_FAILED", "Failed to create order: " + ex.getMessage());
        }
    }
}