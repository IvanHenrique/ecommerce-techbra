package com.ecommerce.inventory.application.port.in;

import java.util.UUID;

public record GetReservationsByOrderQuery(
    UUID orderId
) {
}