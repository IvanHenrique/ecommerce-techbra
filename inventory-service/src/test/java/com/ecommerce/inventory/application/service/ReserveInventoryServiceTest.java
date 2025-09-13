package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.in.ReserveInventoryCommand;
import com.ecommerce.inventory.application.port.in.ReserveInventoryItemCommand;
import com.ecommerce.inventory.application.port.out.InventoryEventPublisherPort;
import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.model.Inventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveInventoryServiceTest {

    @Mock
    private InventoryRepositoryPort inventoryRepository;

    @Mock
    private InventoryEventPublisherPort eventPublisher;

    @InjectMocks
    private ReserveInventoryService reserveInventoryService;

    @Test
    void shouldReserveInventorySuccessfully() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";

        var itemCommand = new ReserveInventoryItemCommand(
                productId,
                "Test Product",
                2
        );

        var command = new ReserveInventoryCommand(
                orderId,
                customerId,
                List.of(itemCommand),
                idempotencyKey
        );

        var inventory = new Inventory(productId, "Test Product", 10);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getValue());
        assertEquals(orderId, result.getValue().orderId());
        assertEquals("RESERVED", result.getValue().status());
        assertFalse(result.getValue().reservedItems().isEmpty());

        verify(inventoryRepository).save(any(Inventory.class));
        verify(eventPublisher).publishInventoryReserved(any());
    }

    @Test
    void shouldFailWhenInsufficientInventory() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        var itemCommand = new ReserveInventoryItemCommand(
                productId,
                "Test Product",
                10
        );

        var command = new ReserveInventoryCommand(
                orderId,
                customerId,
                List.of(itemCommand),
                "test-key"
        );

        var inventory = new Inventory(productId, "Test Product", 5); // Only 5 available

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("INSUFFICIENT_INVENTORY", result.getErrorCode());

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(eventPublisher, never()).publishInventoryReserved(any());
    }

    @Test
    void shouldCreateInventoryForNewProduct() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        var itemCommand = new ReserveInventoryItemCommand(
                productId,
                "New Product",
                1
        );

        var command = new ReserveInventoryCommand(
                orderId,
                customerId,
                List.of(itemCommand),
                "test-key"
        );

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("INSUFFICIENT_INVENTORY", result.getErrorCode());

        verify(inventoryRepository).save(any(Inventory.class)); // Creates inventory with 0 quantity
        verify(eventPublisher, never()).publishInventoryReserved(any());
    }
}