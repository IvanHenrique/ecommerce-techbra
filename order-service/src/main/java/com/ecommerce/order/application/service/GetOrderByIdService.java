package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.in.GetOrderByIdQuery;
import com.ecommerce.order.application.port.in.GetOrderByIdUseCase;
import com.ecommerce.order.application.port.in.GetOrderResponse;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetOrderByIdService implements GetOrderByIdUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetOrderByIdService.class);

    private final OrderRepositoryPort orderRepository;

    public GetOrderByIdService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Cacheable(value = "orders", key = "#query.orderId()")
    public Optional<GetOrderResponse> execute(GetOrderByIdQuery query) {
        logger.info("Getting order by ID from database: {}", query.orderId());
        
        var orderOpt = orderRepository.findById(query.orderId());
        
        if (orderOpt.isEmpty()) {
            logger.warn("Order not found: {}", query.orderId());
            return Optional.empty();
        }
        
        var order = orderOpt.get();
        var response = new GetOrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerId(),
            order.getTotalAmount().amount(),
            order.getTotalAmount().getCurrencyCode(),
            order.getStatus().name(),
            order.getOrderDate()
        );
        
        logger.info("Found order: {}", query.orderId());
        return Optional.of(response);
    }
}