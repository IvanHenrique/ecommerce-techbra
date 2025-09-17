package com.ecommerce.billing.application.service;

import com.ecommerce.billing.application.port.in.ProcessPaymentCommand;
import com.ecommerce.billing.application.port.in.ProcessPaymentResponse;
import com.ecommerce.billing.application.port.in.ProcessPaymentUseCase;
import com.ecommerce.billing.application.port.out.PaymentEventPublisherPort;
import com.ecommerce.billing.application.port.out.PaymentRepositoryPort;
import com.ecommerce.billing.domain.event.PaymentCompletedEvent;
import com.ecommerce.billing.domain.event.PaymentFailedEvent;
import com.ecommerce.billing.domain.model.Payment;
import com.ecommerce.billing.domain.model.PaymentMethod;
import com.ecommerce.shared.domain.common.Result;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPaymentService.class);

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentEventPublisherPort eventPublisher;

    public ProcessPaymentService(PaymentRepositoryPort paymentRepository, 
                               PaymentEventPublisherPort eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "payments", key = "#command.orderId()"),
        @CacheEvict(value = "payment-methods", key = "#command.customerId()")
    })
    public Result<ProcessPaymentResponse> execute(ProcessPaymentCommand command) {
        try {
            logger.info("Processing payment for order: {} with idempotency key: {}", 
                command.orderId(), command.idempotencyKey());

            // Check idempotency
            var existingPayment = paymentRepository.findByIdempotencyKey(command.idempotencyKey());
            if (existingPayment.isPresent()) {
                logger.info("Payment already processed for idempotency key: {}", command.idempotencyKey());
                return createResponseFromPayment(existingPayment.get());
            }

            // Check if payment already exists for this order
            if (paymentRepository.existsByOrderId(command.orderId())) {
                return Result.failure("PAYMENT_ALREADY_EXISTS", 
                    "Payment already exists for order: " + command.orderId());
            }

            // Generate payment reference
            String paymentReference = generatePaymentReference();
            
            // Parse payment method
            PaymentMethod paymentMethodEnum;
            try {
                paymentMethodEnum = PaymentMethod.valueOf(command.paymentMethod().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return Result.failure("INVALID_PAYMENT_METHOD", 
                    "Invalid payment method: " + command.paymentMethod());
            }

            // Create payment
            var payment = new Payment(
                command.orderId(),
                command.customerId(),
                paymentReference,
                command.amount(),
                command.currency(),
                paymentMethodEnum,
                command.idempotencyKey()
            );

            // Simulate payment processing
            boolean paymentSuccessful = simulatePaymentProcessing(payment);

            if (paymentSuccessful) {
                payment.markAsCompleted();
                var savedPayment = paymentRepository.save(payment);
                
                logger.info("Payment completed successfully. Reference: {}", paymentReference);

                // Publish success event
                var event = PaymentCompletedEvent.create(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getCustomerId(),
                    savedPayment.getPaymentReference(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getPaymentMethod().name()
                );
                
                eventPublisher.publishPaymentCompleted(event);
                logger.info("PaymentCompleted event published for order: {}", command.orderId());

                return createResponseFromPayment(savedPayment);

            } else {
                String failureReason = "Payment processing failed - insufficient funds";
                payment.markAsFailed(failureReason);
                var savedPayment = paymentRepository.save(payment);
                
                logger.warn("Payment failed for order: {}. Reason: {}", command.orderId(), failureReason);

                // Publish failure event
                var event = PaymentFailedEvent.create(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getCustomerId(),
                    savedPayment.getPaymentReference(),
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    failureReason
                );
                
                eventPublisher.publishPaymentFailed(event);
                logger.info("PaymentFailed event published for order: {}", command.orderId());

                return createResponseFromPayment(savedPayment);
            }

        } catch (BusinessException ex) {
            logger.error("Business error processing payment: {}", ex.getMessage());
            return Result.failure(ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error processing payment", ex);
            return Result.failure("PAYMENT_PROCESSING_FAILED", "Failed to process payment");
        }
    }

    private boolean simulatePaymentProcessing(Payment payment) {
        // Simulate payment processing logic
        // In real implementation, this would call external payment gateway

        // For amounts <= 100, always succeed (to make tests predictable)
        if (payment.getAmount().doubleValue() <= 100.0) {
            return true;
        }

        // Simulate 80% success rate for demonstration
        // Fail payments with amount > 2000 to simulate insufficient funds
        return payment.getAmount().doubleValue() <= 2000.0 && Math.random() > 0.2;
    }

    private String generatePaymentReference() {
        return "PAY-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Result<ProcessPaymentResponse> createResponseFromPayment(Payment payment) {
        var response = new ProcessPaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getPaymentReference(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus().name(),
            payment.getPaymentMethod().name(),
            payment.getProcessedAt()
        );
        return Result.success(response);
    }
}