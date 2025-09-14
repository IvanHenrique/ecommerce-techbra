package com.ecommerce.bff.application.port.out;

import com.ecommerce.bff.infrastructure.external.dto.OrderDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderServicePort {
    
    List<OrderDto> getOrdersByCustomerId(UUID customerId);
    
    Optional<OrderDto> getOrderById(UUID orderId);
}