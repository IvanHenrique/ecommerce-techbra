package com.ecommerce.billing.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProcessPaymentResponse(
    UUID paymentId,
    UUID orderId,
    String paymentReference,
    BigDecimal amount,
    String currency,
    String status,
    String paymentMethod,
    LocalDateTime processedAt
) {
}