package com.ecommerce.inventory.infrastructure.config;

import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.model.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final InventoryRepositoryPort inventoryRepository;

    public DataInitializer(InventoryRepositoryPort inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        initializeInventoryData();
    }

    private void initializeInventoryData() {
        logger.info("Initializing inventory data...");

        // Create mock products for testing
        createProductIfNotExists(
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), 
            "Premium Product", 
            50
        );
        
        createProductIfNotExists(
            UUID.fromString("987fcdeb-51a2-43d1-9c4e-5f6a789012bc"), 
            "Accessory", 
            100
        );
        
        createProductIfNotExists(
            UUID.fromString("456e7890-e89b-12d3-a456-426614174001"), 
            "Standard Product", 
            75
        );

        logger.info("Inventory data initialization completed");
    }

    private void createProductIfNotExists(UUID productId, String productName, Integer initialStock) {
        if (!inventoryRepository.existsByProductId(productId)) {
            var inventory = new Inventory(productId, productName, initialStock);
            inventoryRepository.save(inventory);
            logger.info("Created inventory for product: {} with {} units", productName, initialStock);
        } else {
            logger.debug("Inventory already exists for product: {}", productName);
        }
    }
}