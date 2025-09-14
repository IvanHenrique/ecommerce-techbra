package com.ecommerce.bff.domain.view;

import com.fasterxml.jackson.annotation.JsonFormat;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime processedAt
) {
}