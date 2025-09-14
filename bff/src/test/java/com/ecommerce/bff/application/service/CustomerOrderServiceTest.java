package com.ecommerce.bff.application.service;

import com.ecommerce.bff.application.port.in.GetCustomerOrdersQuery;
import com.ecommerce.bff.application.port.out.OrderServicePort;
import com.ecommerce.bff.infrastructure.cache.FallbackService;
import com.ecommerce.bff.infrastructure.external.dto.OrderDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

    @Mock
    private OrderServicePort orderServicePort;

    @Mock
    private FallbackService fallbackService;

    @InjectMocks
    private CustomerOrderService customerOrderService;

    @Test
    void shouldGetCustomerOrdersSuccessfully() {
        // Given
        UUID customerId = UUID.randomUUID();
        var query = new GetCustomerOrdersQuery(customerId);
        
        var orderDto = new OrderDto(
            UUID.randomUUID(),
            "ORD-123456",
            customerId,
            new BigDecimal("100.00"),
            "USD",
            "PENDING",
            LocalDateTime.now()
        );
        
        when(orderServicePort.getOrdersByCustomerId(customerId)).thenReturn(List.of(orderDto));

        // When
        var result = customerOrderService.execute(query);

        // Then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(orderDto.orderId(), result.get(0).orderId());
        assertEquals(orderDto.orderNumber(), result.get(0).orderNumber());
        
        verify(orderServicePort).getOrdersByCustomerId(customerId);
        verify(fallbackService, never()).getFallbackOrders(any());
    }

    @Test
    void shouldUseFallbackWhenOrderServiceFails() {
        // Given
        UUID customerId = UUID.randomUUID();
        var query = new GetCustomerOrdersQuery(customerId);
        
        when(orderServicePort.getOrdersByCustomerId(customerId))
            .thenThrow(new RuntimeException("Service unavailable"));
        when(fallbackService.getFallbackOrders(customerId))
            .thenReturn(Collections.emptyList());

        // When
        var result = customerOrderService.execute(query);

        // Then
        assertTrue(result.isEmpty());
        
        verify(orderServicePort).getOrdersByCustomerId(customerId);
        verify(fallbackService).getFallbackOrders(customerId);
    }
}