package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.in.GetReservationResponse;
import com.ecommerce.inventory.application.port.in.GetReservationsByOrderQuery;
import com.ecommerce.inventory.application.port.in.GetReservationsByOrderUseCase;
import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetReservationsByOrderService implements GetReservationsByOrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetReservationsByOrderService.class);

    private final InventoryRepositoryPort inventoryRepository;

    public GetReservationsByOrderService(InventoryRepositoryPort inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Cacheable(value = "stock-reservations", key = "#query.orderId()")
    public List<GetReservationResponse> execute(GetReservationsByOrderQuery query) {
        logger.info("Getting reservations for order from database: {}", query.orderId());
        
        var inventories = inventoryRepository.findAll();
        
        var reservations = inventories.stream()
            .flatMap(inventory -> inventory.getReservations().stream())
            .filter(reservation -> reservation.getOrderId().equals(query.orderId()))
            .map(reservation -> new GetReservationResponse(
                reservation.getInventory().getProductId(),
                reservation.getInventory().getProductName(),
                reservation.getQuantity(),
                reservation.getReservationReference(),
                reservation.getStatus().name()
            ))
            .toList();
        
        logger.info("Found {} reservations for order: {}", reservations.size(), query.orderId());
        return reservations;
    }
}