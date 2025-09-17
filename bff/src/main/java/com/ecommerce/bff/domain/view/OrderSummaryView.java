package com.ecommerce.bff.domain.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryView(
    UUID orderId,
    String orderNumber,
    BigDecimal totalAmount,
    String currency,
    String status,
    LocalDateTime orderDate
) {
}