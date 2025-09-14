package com.ecommerce.inventory.adapter.in.web;

import com.ecommerce.inventory.application.port.in.GetReservationsByOrderQuery;
import com.ecommerce.inventory.application.port.in.GetReservationsByOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Inventory", description = "Inventory reservation operations")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final GetReservationsByOrderUseCase getReservationsByOrderUseCase;

    public InventoryController(GetReservationsByOrderUseCase getReservationsByOrderUseCase) {
        this.getReservationsByOrderUseCase = getReservationsByOrderUseCase;
    }

    @GetMapping("/reservations/order/{orderId}")
    @Operation(summary = "Get reservations by order ID", description = "Retrieves inventory reservations for a specific order")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByOrderId(@PathVariable UUID orderId) {
        logger.info("Received request to get reservations for order: {}", orderId);

        var query = new GetReservationsByOrderQuery(orderId);
        var reservations = getReservationsByOrderUseCase.execute(query);

        var response = reservations.stream()
                .map(reservation -> new ReservationResponseDto(
                        reservation.productId(),
                        reservation.productName(),
                        reservation.quantityReserved(),
                        reservation.reservationReference(),
                        reservation.status()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}