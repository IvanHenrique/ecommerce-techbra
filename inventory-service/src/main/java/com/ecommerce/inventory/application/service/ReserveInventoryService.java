package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.in.*;
import com.ecommerce.inventory.application.port.out.InventoryEventPublisherPort;
import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.event.InventoryReservedEvent;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.shared.domain.common.Result;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class ReserveInventoryService implements ReserveInventoryUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReserveInventoryService.class);

    private final InventoryRepositoryPort inventoryRepository;
    private final InventoryEventPublisherPort eventPublisher;

    public ReserveInventoryService(InventoryRepositoryPort inventoryRepository,
                                 InventoryEventPublisherPort eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result<ReserveInventoryResponse> execute(ReserveInventoryCommand command) {
        try {
            logger.info("Reserving inventory for order: {} with idempotency key: {}", 
                command.orderId(), command.idempotencyKey());

            // Check if reservation already exists (idempotency)
            // For MVP, we'll use a simpler approach - check if product already reserved for this order
            var existingReservations = new ArrayList<Inventory>();
            for (var itemCommand : command.items()) {
                var inventory = inventoryRepository.findByProductId(itemCommand.productId());
                if (inventory.isPresent()) {
                    var existing = inventory.get().getReservations().stream()
                        .anyMatch(r -> r.getOrderId().equals(command.orderId()));
                    if (existing) {
                        logger.info("Inventory already reserved for order: {}", command.orderId());
                        return createResponseFromExistingReservations(command.orderId(), existingReservations);
                    }
                    existingReservations.add(inventory.get());
                }
            }

            // Generate reservation reference
            String reservationReference = generateReservationReference();
            
            var reservedItems = new ArrayList<ReservedItemResponse>();

            // Reserve each item
            for (var itemCommand : command.items()) {
                var inventoryOpt = inventoryRepository.findByProductId(itemCommand.productId());
                
                if (inventoryOpt.isEmpty()) {
                    // Create inventory with zero quantity if product doesn't exist
                    var newInventory = new Inventory(itemCommand.productId(), itemCommand.productName(), 0);
                    inventoryRepository.save(newInventory);
                    
                    return Result.failure("INSUFFICIENT_INVENTORY", 
                        "Product " + itemCommand.productName() + " is out of stock");
                }

                var inventory = inventoryOpt.get();
                
                // Check if can reserve
                if (!inventory.canReserve(itemCommand.quantity())) {
                    return Result.failure("INSUFFICIENT_INVENTORY", 
                        "Insufficient inventory for product: " + itemCommand.productName() + 
                        ". Available: " + inventory.getAvailableQuantity() + 
                        ", Requested: " + itemCommand.quantity());
                }

                // Reserve inventory
                inventory.reserve(itemCommand.quantity(), command.orderId(), reservationReference);
                var savedInventory = inventoryRepository.save(inventory);
                
                logger.info("Reserved {} units of product {} for order {}", 
                    itemCommand.quantity(), itemCommand.productName(), command.orderId());

                // Publish inventory reserved event
                var event = InventoryReservedEvent.create(
                    savedInventory.getId(),
                    command.orderId(),
                    savedInventory.getProductId(),
                    savedInventory.getProductName(),
                    itemCommand.quantity(),
                    reservationReference
                );
                
                eventPublisher.publishInventoryReserved(event);
                logger.info("InventoryReserved event published for order: {} product: {}", 
                    command.orderId(), itemCommand.productName());

                // Add to response
                reservedItems.add(new ReservedItemResponse(
                    savedInventory.getProductId(),
                    savedInventory.getProductName(),
                    itemCommand.quantity(),
                    savedInventory.getAvailableQuantity()
                ));
            }

            var response = new ReserveInventoryResponse(
                command.orderId(),
                reservationReference,
                reservedItems,
                "RESERVED",
                LocalDateTime.now()
            );

            logger.info("Inventory reservation completed for order: {}. Reference: {}", 
                command.orderId(), reservationReference);

            return Result.success(response);

        } catch (BusinessException ex) {
            logger.error("Business error reserving inventory: {}", ex.getMessage());
            return Result.failure(ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error reserving inventory", ex);
            return Result.failure("INVENTORY_RESERVATION_FAILED", "Failed to reserve inventory");
        }
    }

    private String generateReservationReference() {
        return "RES-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Result<ReserveInventoryResponse> createResponseFromExistingReservations(UUID orderId, 
                                                                                   java.util.List<Inventory> inventories) {
        var reservedItems = new ArrayList<ReservedItemResponse>();
        
        for (var inventory : inventories) {
            var reservation = inventory.getReservations().stream()
                .filter(r -> r.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
                
            if (reservation != null) {
                reservedItems.add(new ReservedItemResponse(
                    inventory.getProductId(),
                    inventory.getProductName(),
                    reservation.getQuantity(),
                    inventory.getAvailableQuantity()
                ));
            }
        }

        var response = new ReserveInventoryResponse(
            orderId,
            inventories.isEmpty() ? "N/A" : inventories.get(0).getReservations().get(0).getReservationReference(),
            reservedItems,
            "ALREADY_RESERVED",
            LocalDateTime.now()
        );

        return Result.success(response);
    }
}