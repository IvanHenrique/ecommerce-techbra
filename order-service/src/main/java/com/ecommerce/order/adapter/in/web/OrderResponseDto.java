package com.ecommerce.order.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponseDto(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    BigDecimal totalAmount,
    String currency,
    String status,
    LocalDateTime orderDate
) {
}