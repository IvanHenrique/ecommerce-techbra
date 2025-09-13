package com.ecommerce.inventory.adapter.out.persistence;

import com.ecommerce.inventory.domain.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryJpaRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);
    
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
    List<Inventory> findByProductIdIn(@Param("productIds") List<UUID> productIds);
    
    boolean existsByProductId(UUID productId);
    
    @Query("SELECT i FROM Inventory i WHERE i.status = 'OUT_OF_STOCK'")
    List<Inventory> findOutOfStockItems();
}