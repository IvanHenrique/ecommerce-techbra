package com.ecommerce.bff.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateOrderResponse(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    BigDecimal totalAmount,
    String currency,
    String status,
    LocalDateTime orderDate
) {
}