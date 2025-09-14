package com.ecommerce.bff.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDto(
    @JsonProperty("paymentId") UUID paymentId,
    @JsonProperty("orderId") UUID orderId,
    @JsonProperty("paymentReference") String paymentReference,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency,
    @JsonProperty("status") String status,
    @JsonProperty("paymentMethod") String paymentMethod,
    @JsonProperty("processedAt") LocalDateTime processedAt
) {
}