package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.in.GetOrderResponse;
import com.ecommerce.order.application.port.in.GetOrdersByCustomerQuery;
import com.ecommerce.order.application.port.in.GetOrdersByCustomerUseCase;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetOrdersByCustomerService implements GetOrdersByCustomerUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetOrdersByCustomerService.class);

    private final OrderRepositoryPort orderRepository;

    public GetOrdersByCustomerService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<GetOrderResponse> execute(GetOrdersByCustomerQuery query) {
        logger.info("Getting orders for customer: {}", query.customerId());
        
        var orders = orderRepository.findByCustomerId(query.customerId());
        
        var response = orders.stream()
            .map(order -> new GetOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotalAmount().amount(),
                order.getTotalAmount().getCurrencyCode(),
                order.getStatus().name(),
                order.getOrderDate()
            ))
            .toList();
        
        logger.info("Found {} orders for customer: {}", response.size(), query.customerId());
        return response;
    }
}