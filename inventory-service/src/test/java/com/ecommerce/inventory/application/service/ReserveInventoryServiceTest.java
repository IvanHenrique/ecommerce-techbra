package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.in.ReserveInventoryCommand;
import com.ecommerce.inventory.application.port.in.ReserveInventoryItemCommand;
import com.ecommerce.inventory.application.port.out.InventoryEventPublisherPort;
import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InventoryReservation;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
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

    @Test
    void shouldReturnExistingReservationWhenIdempotencyKeyMatches() {
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

        // Mock InventoryReservation
        var existingReservation = mock(InventoryReservation.class);
        when(existingReservation.getOrderId()).thenReturn(orderId);

        // Mock inventory with existing reservation
        var inventory = mock(Inventory.class);
        when(inventory.getReservations()).thenReturn(List.of(existingReservation));

        // Mock repository
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(orderId, result.getValue().orderId());
        assertEquals("ALREADY_RESERVED", result.getValue().status());

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(eventPublisher, never()).publishInventoryReserved(any());
    }

    @Test
    void shouldFailWhenBusinessExceptionOccurs() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        var itemCommand = new ReserveInventoryItemCommand(
                productId,
                "Test Product",
                2
        );

        var command = new ReserveInventoryCommand(
                orderId,
                customerId,
                List.of(itemCommand),
                "test-key"
        );

        var inventory = new Inventory(productId, "Test Product", 10);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenThrow(new BusinessException("INVENTORY_LOCK_ERROR", "Could not lock inventory"));

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("INVENTORY_LOCK_ERROR", result.getErrorCode());
        assertEquals("Could not lock inventory", result.getErrorMessage());

        verify(inventoryRepository).save(any(Inventory.class));
        verify(eventPublisher, never()).publishInventoryReserved(any());
    }

    @Test
    void shouldFailWhenUnexpectedExceptionOccurs() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        var itemCommand = new ReserveInventoryItemCommand(
                productId,
                "Test Product",
                2
        );

        var command = new ReserveInventoryCommand(
                orderId,
                customerId,
                List.of(itemCommand),
                "test-key"
        );

        when(inventoryRepository.findByProductId(productId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("INVENTORY_RESERVATION_FAILED", result.getErrorCode());
        assertEquals("Failed to reserve inventory", result.getErrorMessage());

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(eventPublisher, never()).publishInventoryReserved(any());
    }

    @Test
    void shouldFailWhenEventPublishingFails() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        var itemCommand = new ReserveInventoryItemCommand(
                productId,
                "Test Product",
                2
        );

        var command = new ReserveInventoryCommand(
                orderId,
                customerId,
                List.of(itemCommand),
                "test-key"
        );

        var inventory = new Inventory(productId, "Test Product", 10);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // Event publisher throws exception
        doThrow(new RuntimeException("Kafka broker unavailable"))
                .when(eventPublisher).publishInventoryReserved(any());

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("INVENTORY_RESERVATION_FAILED", result.getErrorCode());
        assertEquals("Failed to reserve inventory", result.getErrorMessage());

        verify(inventoryRepository).save(any(Inventory.class));
        verify(eventPublisher).publishInventoryReserved(any());
    }

    @Test
    void shouldHandleCreateResponseFromExistingReservationsWithEmptyList() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        var itemCommand = new ReserveInventoryItemCommand(
                productId,
                "Test Product",
                2
        );

        var command = new ReserveInventoryCommand(
                orderId,
                customerId,
                List.of(itemCommand),
                "test-key"
        );

        // Mock inventory with existing reservation but getReservations returns empty list in createResponseFromExistingReservations
        var inventory = mock(Inventory.class);
        var existingReservation = mock(InventoryReservation.class);

        when(inventory.getReservations()).thenReturn(List.of(existingReservation))
                .thenReturn(List.of()); // Second call in createResponseFromExistingReservations returns empty

        when(existingReservation.getOrderId()).thenReturn(orderId);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        // When
        var result = reserveInventoryService.execute(command);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("ALREADY_RESERVED", result.getValue().status());
        assertEquals("N/A", result.getValue().reservationReference());
        assertTrue(result.getValue().reservedItems().isEmpty());
    }
}