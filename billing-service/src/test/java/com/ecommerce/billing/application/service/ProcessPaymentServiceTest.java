package com.ecommerce.billing.application.service;

import com.ecommerce.billing.application.port.in.ProcessPaymentCommand;
import com.ecommerce.billing.application.port.out.PaymentEventPublisherPort;
import com.ecommerce.billing.application.port.out.PaymentRepositoryPort;
import com.ecommerce.billing.domain.model.Payment;
import com.ecommerce.billing.domain.model.PaymentMethod;
import com.ecommerce.billing.domain.model.PaymentStatus;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @Mock
    private PaymentEventPublisherPort eventPublisher;

    @InjectMocks
    private ProcessPaymentService processPaymentService;

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";

        var command = new ProcessPaymentCommand(
                orderId,
                customerId,
                new BigDecimal("100.00"),
                "USD",
                "CREDIT_CARD",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            // Mock the ID that would be set by JPA
            var paymentMock = mock(Payment.class);
            when(paymentMock.getId()).thenReturn(UUID.randomUUID());
            when(paymentMock.getOrderId()).thenReturn(payment.getOrderId());
            when(paymentMock.getCustomerId()).thenReturn(payment.getCustomerId());
            when(paymentMock.getPaymentReference()).thenReturn("PAY-123456789-ABCD1234");
            when(paymentMock.getAmount()).thenReturn(payment.getAmount());
            when(paymentMock.getCurrency()).thenReturn(payment.getCurrency());
            when(paymentMock.getStatus()).thenReturn(PaymentStatus.COMPLETED);
            when(paymentMock.getPaymentMethod()).thenReturn(payment.getPaymentMethod());
            when(paymentMock.getProcessedAt()).thenReturn(java.time.LocalDateTime.now());
            return paymentMock;
        });

        // When
        var result = processPaymentService.execute(command);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getValue());
        assertEquals(orderId, result.getValue().orderId());
        assertEquals("USD", result.getValue().currency());

        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishPaymentCompleted(any());
    }

    @Test
    void shouldReturnExistingPaymentWhenIdempotencyKeyExists() {
        // Given
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "existing-key";

        var existingPayment = mock(Payment.class);
        when(existingPayment.getId()).thenReturn(UUID.randomUUID());
        when(existingPayment.getOrderId()).thenReturn(orderId);
        when(existingPayment.getPaymentReference()).thenReturn("PAY-EXISTING-REF");
        when(existingPayment.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(existingPayment.getCurrency()).thenReturn("USD");
        when(existingPayment.getStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(existingPayment.getPaymentMethod()).thenReturn(PaymentMethod.CREDIT_CARD);
        when(existingPayment.getProcessedAt()).thenReturn(java.time.LocalDateTime.now());

        var command = new ProcessPaymentCommand(
                orderId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "USD",
                "CREDIT_CARD",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        // When
        var result = processPaymentService.execute(command);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(orderId, result.getValue().orderId());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(eventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void shouldProcessPaymentWithFailure() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";

        // Use amount > 2000 to force payment failure in simulatePaymentProcessing
        var command = new ProcessPaymentCommand(
                orderId,
                customerId,
                new BigDecimal("3000.00"),
                "USD",
                "CREDIT_CARD",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            var paymentMock = mock(Payment.class);
            when(paymentMock.getId()).thenReturn(UUID.randomUUID());
            when(paymentMock.getOrderId()).thenReturn(payment.getOrderId());
            when(paymentMock.getCustomerId()).thenReturn(payment.getCustomerId());
            when(paymentMock.getPaymentReference()).thenReturn("PAY-123456789-ABCD1234");
            when(paymentMock.getAmount()).thenReturn(payment.getAmount());
            when(paymentMock.getCurrency()).thenReturn(payment.getCurrency());
            when(paymentMock.getStatus()).thenReturn(PaymentStatus.FAILED);
            when(paymentMock.getPaymentMethod()).thenReturn(payment.getPaymentMethod());
            when(paymentMock.getProcessedAt()).thenReturn(java.time.LocalDateTime.now());
            return paymentMock;
        });

        // When
        var result = processPaymentService.execute(command);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getValue());
        assertEquals(orderId, result.getValue().orderId());
        assertEquals("FAILED", result.getValue().status());

        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publishPaymentFailed(any());
        verify(eventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void shouldFailWhenPaymentAlreadyExistsForOrder() {
        // Given
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";

        var command = new ProcessPaymentCommand(
                orderId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "USD",
                "CREDIT_CARD",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        // When
        var result = processPaymentService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("PAYMENT_ALREADY_EXISTS", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("Payment already exists for order"));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(eventPublisher, never()).publishPaymentCompleted(any());
        verify(eventPublisher, never()).publishPaymentFailed(any());
    }

    @Test
    void shouldFailWhenInvalidPaymentMethod() {
        // Given
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";

        var command = new ProcessPaymentCommand(
                orderId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "USD",
                "INVALID_METHOD",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);

        // When
        var result = processPaymentService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("INVALID_PAYMENT_METHOD", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("Invalid payment method"));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(eventPublisher, never()).publishPaymentCompleted(any());
        verify(eventPublisher, never()).publishPaymentFailed(any());
    }

    @Test
    void shouldFailWhenBusinessExceptionOccurs() {
        // Given
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";

        var command = new ProcessPaymentCommand(
                orderId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "USD",
                "CREDIT_CARD",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new BusinessException("PAYMENT_GATEWAY_ERROR", "Payment gateway unavailable"));

        // When
        var result = processPaymentService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("PAYMENT_GATEWAY_ERROR", result.getErrorCode());
        assertEquals("Payment gateway unavailable", result.getErrorMessage());

        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher, never()).publishPaymentCompleted(any());
        verify(eventPublisher, never()).publishPaymentFailed(any());
    }

    @Test
    void shouldFailWhenUnexpectedExceptionOccurs() {
        // Given
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "test-key-123";

        var command = new ProcessPaymentCommand(
                orderId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "USD",
                "CREDIT_CARD",
                idempotencyKey
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        var result = processPaymentService.execute(command);

        // Then
        assertTrue(result.isFailure());
        assertEquals("PAYMENT_PROCESSING_FAILED", result.getErrorCode());
        assertEquals("Failed to process payment", result.getErrorMessage());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(eventPublisher, never()).publishPaymentCompleted(any());
        verify(eventPublisher, never()).publishPaymentFailed(any());
    }
}