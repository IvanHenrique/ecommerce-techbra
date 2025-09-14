package com.ecommerce.order.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetOrderResponse(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    BigDecimal totalAmount,
    String currency,
    String status,
    LocalDateTime orderDate
) {
}