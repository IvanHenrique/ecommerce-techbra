package com.ecommerce.inventory.application.port.out;

import com.ecommerce.inventory.domain.model.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepositoryPort {
    
    Inventory save(Inventory inventory);
    
    Optional<Inventory> findById(UUID id);
    
    Optional<Inventory> findByProductId(UUID productId);
    
    List<Inventory> findByProductIds(List<UUID> productIds);
    
    List<Inventory> findAll();
    
    boolean existsByProductId(UUID productId);
}