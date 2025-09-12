package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.event.OrderCreatedEvent;

public interface OrderEventPublisherPort {
    
    void publishOrderCreated(OrderCreatedEvent event);
}