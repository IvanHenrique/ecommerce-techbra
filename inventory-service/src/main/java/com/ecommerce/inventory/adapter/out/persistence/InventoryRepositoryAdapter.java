package com.ecommerce.inventory.adapter.out.persistence;

import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.model.Inventory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryRepositoryAdapter implements InventoryRepositoryPort {

    private final InventoryJpaRepository jpaRepository;

    public InventoryRepositoryAdapter(InventoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Inventory save(Inventory inventory) {
        return jpaRepository.save(inventory);
    }

    @Override
    public Optional<Inventory> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Inventory> findByProductId(UUID productId) {
        return jpaRepository.findByProductId(productId);
    }

    @Override
    public List<Inventory> findByProductIds(List<UUID> productIds) {
        return jpaRepository.findByProductIdIn(productIds);
    }

    @Override
    public List<Inventory> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsByProductId(UUID productId) {
        return jpaRepository.existsByProductId(productId);
    }
}