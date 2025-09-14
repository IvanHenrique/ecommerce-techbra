package com.ecommerce.order.application.port.in;

import java.util.UUID;

public record GetOrdersByCustomerQuery(
    UUID customerId
) {
}