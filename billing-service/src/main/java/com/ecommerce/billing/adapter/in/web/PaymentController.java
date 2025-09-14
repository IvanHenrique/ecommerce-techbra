package com.ecommerce.billing.adapter.in.web;

import com.ecommerce.billing.application.port.in.*;
import com.ecommerce.shared.infrastructure.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments", description = "Payment information operations")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final GetPaymentByOrderUseCase getPaymentByOrderUseCase;
    private final GetPaymentsByCustomerUseCase getPaymentsByCustomerUseCase;

    public PaymentController(GetPaymentByOrderUseCase getPaymentByOrderUseCase,
                             GetPaymentsByCustomerUseCase getPaymentsByCustomerUseCase) {
        this.getPaymentByOrderUseCase = getPaymentByOrderUseCase;
        this.getPaymentsByCustomerUseCase = getPaymentsByCustomerUseCase;
    }

    @GetMapping("/payments/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Retrieves payment information for a specific order")
    public ResponseEntity<PaymentResponseDto> getPaymentByOrderId(@PathVariable UUID orderId) {
        logger.info("Received request to get payment for order: {}", orderId);

        var query = new GetPaymentByOrderQuery(orderId);
        var paymentOpt = getPaymentByOrderUseCase.execute(query);

        if (paymentOpt.isEmpty()) {
            throw new BusinessException("PAYMENT_NOT_FOUND", "Payment not found for order: " + orderId);
        }

        var payment = paymentOpt.get();
        var response = new PaymentResponseDto(
                payment.paymentId(),
                payment.orderId(),
                payment.paymentReference(),
                payment.amount(),
                payment.currency(),
                payment.status(),
                payment.paymentMethod(),
                payment.processedAt()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/customers/{customerId}/payments")
    @Operation(summary = "Get payments by customer", description = "Retrieves all payments for a specific customer")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByCustomer(@PathVariable UUID customerId) {
        logger.info("Received request to get payments for customer: {}", customerId);

        var query = new GetPaymentsByCustomerQuery(customerId);
        var payments = getPaymentsByCustomerUseCase.execute(query);

        var response = payments.stream()
                .map(payment -> new PaymentResponseDto(
                        payment.paymentId(),
                        payment.orderId(),
                        payment.paymentReference(),
                        payment.amount(),
                        payment.currency(),
                        payment.status(),
                        payment.paymentMethod(),
                        payment.processedAt()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}