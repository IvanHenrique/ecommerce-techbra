package com.ecommerce.bff.application.service;

import com.ecommerce.bff.application.port.in.GetOrderDetailsQuery;
import com.ecommerce.bff.application.port.out.BillingServicePort;
import com.ecommerce.bff.application.port.out.InventoryServicePort;
import com.ecommerce.bff.application.port.out.OrderServicePort;
import com.ecommerce.bff.infrastructure.cache.FallbackService;
import com.ecommerce.bff.infrastructure.external.dto.OrderDto;
import com.ecommerce.bff.infrastructure.external.dto.PaymentDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDetailsServiceTest {

    @Mock
    private OrderServicePort orderServicePort;

    @Mock
    private BillingServicePort billingServicePort;

    @Mock
    private InventoryServicePort inventoryServicePort;

    @Mock
    private FallbackService fallbackService;

    @InjectMocks
    private OrderDetailsService orderDetailsService;

    @Test
    void shouldGetOrderDetailsSuccessfully() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);
        
        var orderDto = new OrderDto(
            orderId,
            "ORD-123456",
            customerId,
            new BigDecimal("100.00"),
            "USD",
            "PENDING",
            LocalDateTime.now()
        );
        
        var paymentDto = new PaymentDto(
            UUID.randomUUID(),
            orderId,
            "PAY-123456",
            new BigDecimal("100.00"),
            "USD",
            "COMPLETED",
            "CREDIT_CARD",
            LocalDateTime.now()
        );
        
        when(orderServicePort.getOrderById(orderId)).thenReturn(Optional.of(orderDto));
        when(billingServicePort.getPaymentByOrderId(orderId)).thenReturn(Optional.of(paymentDto));
        when(inventoryServicePort.getReservationsByOrderId(orderId)).thenReturn(Collections.emptyList());

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().orderId());
        assertEquals("ORD-123456", result.get().orderNumber());
        assertNotNull(result.get().payment());
        assertEquals("COMPLETED", result.get().payment().status());
        
        verify(orderServicePort).getOrderById(orderId);
        verify(billingServicePort).getPaymentByOrderId(orderId);
        verify(inventoryServicePort).getReservationsByOrderId(orderId);
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);
        
        when(orderServicePort.getOrderById(orderId)).thenReturn(Optional.empty());

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isEmpty());
        
        verify(orderServicePort).getOrderById(orderId);
        verify(billingServicePort, never()).getPaymentByOrderId(any());
        verify(inventoryServicePort, never()).getReservationsByOrderId(any());
    }

    @Test
    void shouldUseFallbackForPaymentWhenServiceFails() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);
        
        var orderDto = new OrderDto(
            orderId,
            "ORD-123456",
            customerId,
            new BigDecimal("100.00"),
            "USD",
            "PENDING",
            LocalDateTime.now()
        );
        
        when(orderServicePort.getOrderById(orderId)).thenReturn(Optional.of(orderDto));
        when(billingServicePort.getPaymentByOrderId(orderId))
            .thenThrow(new RuntimeException("Service unavailable"));
        when(fallbackService.getFallbackPayment(orderId))
            .thenReturn(null);
        when(inventoryServicePort.getReservationsByOrderId(orderId)).thenReturn(Collections.emptyList());

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().orderId());
        
        verify(orderServicePort).getOrderById(orderId);
        verify(billingServicePort).getPaymentByOrderId(orderId);
        verify(fallbackService).getFallbackPayment(orderId);
    }

    @Test
    void shouldUseFallbackForInventoryWhenServiceFails() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);

        var orderDto = new OrderDto(
                orderId,
                "ORD-123456",
                customerId,
                new BigDecimal("100.00"),
                "USD",
                "PENDING",
                LocalDateTime.now()
        );

        var paymentDto = new PaymentDto(
                UUID.randomUUID(),
                orderId,
                "PAY-123456",
                new BigDecimal("100.00"),
                "USD",
                "COMPLETED",
                "CREDIT_CARD",
                LocalDateTime.now()
        );

        when(orderServicePort.getOrderById(orderId)).thenReturn(Optional.of(orderDto));
        when(billingServicePort.getPaymentByOrderId(orderId)).thenReturn(Optional.of(paymentDto));
        when(inventoryServicePort.getReservationsByOrderId(orderId))
                .thenThrow(new RuntimeException("Inventory service unavailable"));
        when(fallbackService.getFallbackInventory(orderId))
                .thenReturn(Collections.emptyList());

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().orderId());
        assertTrue(result.get().inventory().isEmpty());

        verify(orderServicePort).getOrderById(orderId);
        verify(billingServicePort).getPaymentByOrderId(orderId);
        verify(inventoryServicePort).getReservationsByOrderId(orderId);
        verify(fallbackService).getFallbackInventory(orderId);
    }

    @Test
    void shouldUseFallbackForBothPaymentAndInventoryWhenServicesFail() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);

        var orderDto = new OrderDto(
                orderId,
                "ORD-123456",
                customerId,
                new BigDecimal("100.00"),
                "USD",
                "PENDING",
                LocalDateTime.now()
        );

        when(orderServicePort.getOrderById(orderId)).thenReturn(Optional.of(orderDto));
        when(billingServicePort.getPaymentByOrderId(orderId))
                .thenThrow(new RuntimeException("Billing service unavailable"));
        when(inventoryServicePort.getReservationsByOrderId(orderId))
                .thenThrow(new RuntimeException("Inventory service unavailable"));
        when(fallbackService.getFallbackPayment(orderId)).thenReturn(null);
        when(fallbackService.getFallbackInventory(orderId)).thenReturn(Collections.emptyList());

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().orderId());
        assertNull(result.get().payment());
        assertTrue(result.get().inventory().isEmpty());

        verify(fallbackService).getFallbackPayment(orderId);
        verify(fallbackService).getFallbackInventory(orderId);
    }

    @Test
    void shouldUseFallbackWhenOrderServiceFails() {
        // Given
        UUID orderId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);

        when(orderServicePort.getOrderById(orderId))
                .thenThrow(new RuntimeException("Order service unavailable"));
        when(fallbackService.getFallbackOrderDetails(orderId))
                .thenReturn(Optional.empty());

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isEmpty());

        verify(orderServicePort).getOrderById(orderId);
        verify(fallbackService).getFallbackOrderDetails(orderId);
        verify(billingServicePort, never()).getPaymentByOrderId(any());
        verify(inventoryServicePort, never()).getReservationsByOrderId(any());
    }

    @Test
    void shouldHandlePaymentNotFoundScenario() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);

        var orderDto = new OrderDto(
                orderId,
                "ORD-123456",
                customerId,
                new BigDecimal("100.00"),
                "USD",
                "PENDING",
                LocalDateTime.now()
        );

        when(orderServicePort.getOrderById(orderId)).thenReturn(Optional.of(orderDto));
        when(billingServicePort.getPaymentByOrderId(orderId)).thenReturn(Optional.empty());
        when(inventoryServicePort.getReservationsByOrderId(orderId)).thenReturn(Collections.emptyList());

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().orderId());
        assertNull(result.get().payment());

        verify(orderServicePort).getOrderById(orderId);
        verify(billingServicePort).getPaymentByOrderId(orderId);
        verify(inventoryServicePort).getReservationsByOrderId(orderId);
        verify(fallbackService, never()).getFallbackPayment(any());
    }

    @Test
    void shouldUseFallbackOrderDetailsWhenUnexpectedExceptionOccurs() {
        // Given
        UUID orderId = UUID.randomUUID();
        var query = new GetOrderDetailsQuery(orderId);

        var fallbackOrderView = mock(com.ecommerce.bff.domain.view.CustomerOrderView.class);

        when(orderServicePort.getOrderById(orderId))
                .thenThrow(new RuntimeException("Unexpected error"));
        when(fallbackService.getFallbackOrderDetails(orderId))
                .thenReturn(Optional.of(fallbackOrderView));

        // When
        var result = orderDetailsService.execute(query);

        // Then
        assertTrue(result.isPresent());
        assertEquals(fallbackOrderView, result.get());

        verify(orderServicePort).getOrderById(orderId);
        verify(fallbackService).getFallbackOrderDetails(orderId);
    }
}