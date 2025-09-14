package com.ecommerce.bff.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderDto(
    @JsonProperty("orderId") UUID orderId,
    @JsonProperty("orderNumber") String orderNumber,
    @JsonProperty("customerId") UUID customerId,
    @JsonProperty("totalAmount") BigDecimal totalAmount,
    @JsonProperty("currency") String currency,
    @JsonProperty("status") String status,
    @JsonProperty("orderDate") LocalDateTime orderDate
) {
}