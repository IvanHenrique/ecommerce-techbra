package com.ecommerce.billing.application.port.in;

import java.util.UUID;

public record GetPaymentByOrderQuery(
    UUID orderId
) {
}