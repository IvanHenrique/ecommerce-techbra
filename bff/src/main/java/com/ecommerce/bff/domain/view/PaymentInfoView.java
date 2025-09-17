package com.ecommerce.bff.domain.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentInfoView(
    UUID paymentId,
    String paymentReference,
    BigDecimal amount,
    String currency,
    String status,
    String paymentMethod,
    LocalDateTime processedAt
) {
}