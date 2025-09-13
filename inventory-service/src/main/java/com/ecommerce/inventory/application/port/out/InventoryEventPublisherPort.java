package com.ecommerce.inventory.application.port.out;

import com.ecommerce.inventory.domain.event.InventoryReleasedEvent;
import com.ecommerce.inventory.domain.event.InventoryReservedEvent;

public interface InventoryEventPublisherPort {
    
    void publishInventoryReserved(InventoryReservedEvent event);
    
    void publishInventoryReleased(InventoryReleasedEvent event);
}