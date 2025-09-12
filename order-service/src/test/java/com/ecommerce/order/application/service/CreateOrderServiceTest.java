package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.in.CreateOrderCommand;
import com.ecommerce.order.application.port.in.CreateOrderItemCommand;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.domain.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderEventPublisherPort eventPublisher;

    @InjectMocks
    private CreateOrderService createOrderService;

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        var itemCommand = new CreateOrderItemCommand(
            productId,
            "Test Product",
            2,
            new BigDecimal("50.00"),
            "USD"
        );
        
        var command = new CreateOrderCommand(customerId, List.of(itemCommand));
        
        // Mock order after save (with generated ID)
        var savedOrder = mock(Order.class);
        when(savedOrder.getId()).thenReturn(UUID.randomUUID());
        when(savedOrder.getOrderNumber()).thenReturn("ORD-123456");
        when(savedOrder.getCustomerId()).thenReturn(customerId);
        
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        var result = createOrderService.execute(command);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getValue());
        assertEquals(customerId, result.getValue().customerId());
        
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderCreated(any());
    }

    @Test
    void shouldFailWhenOrderNumberExists() {
        // Given
        UUID customerId = UUID.randomUUID();
        var itemCommand = new CreateOrderItemCommand(
            UUID.randomUUID(),
            "Test Product",
            1,
            new BigDecimal("25.00"),
            "USD"
        );
        
        var command = new CreateOrderCommand(customerId, List.of(itemCommand));
        
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(true);

        // When
        var result = createOrderService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("ORDER_NUMBER_EXISTS", result.getErrorCode());
        
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishOrderCreated(any());
    }
}