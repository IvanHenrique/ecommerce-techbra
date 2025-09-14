package com.ecommerce.bff.application.port.out;

import com.ecommerce.bff.infrastructure.external.dto.InventoryReservationDto;

import java.util.List;
import java.util.UUID;

public interface InventoryServicePort {
    
    List<InventoryReservationDto> getReservationsByOrderId(UUID orderId);
}