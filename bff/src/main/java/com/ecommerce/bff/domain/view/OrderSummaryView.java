package com.ecommerce.bff.domain.view;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryView(
    UUID orderId,
    String orderNumber,
    BigDecimal totalAmount,
    String currency,
    String status,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime orderDate
) {
}