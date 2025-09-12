package com.ecommerce.shared.domain.event;

public interface DomainEventPublisher {
    
    void publish(DomainEvent event);
    
    void publishAll(DomainEvent... events);
}