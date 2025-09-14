package com.ecommerce.billing.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponseDto(
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