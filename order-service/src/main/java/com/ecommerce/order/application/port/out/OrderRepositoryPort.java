package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    
    Order save(Order order);
    
    Optional<Order> findById(UUID id);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByCustomerId(UUID customerId);
    
    void deleteById(UUID id);
    
    boolean existsByOrderNumber(String orderNumber);
}