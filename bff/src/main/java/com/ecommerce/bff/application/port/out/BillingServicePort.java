package com.ecommerce.bff.application.port.out;

import com.ecommerce.bff.infrastructure.external.dto.PaymentDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingServicePort {
    
    Optional<PaymentDto> getPaymentByOrderId(UUID orderId);
    
    List<PaymentDto> getPaymentsByCustomerId(UUID customerId);
}