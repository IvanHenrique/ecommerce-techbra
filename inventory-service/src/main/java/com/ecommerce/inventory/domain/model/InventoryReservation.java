package com.ecommerce.inventory.domain.model;

import com.ecommerce.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reservation_reference", nullable = false)
    private String reservationReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    protected InventoryReservation() {
        // JPA Constructor
    }

    public InventoryReservation(UUID orderId, Integer quantity, String reservationReference) {
        this.orderId = orderId;
        this.quantity = quantity;
        this.reservationReference = reservationReference;
        this.status = ReservationStatus.PENDING;
        this.expiresAt = LocalDateTime.now().plusHours(24); // 24h expiry
    }

    public void confirm() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Confirmed reservations cannot be cancelled");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        if (this.status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.EXPIRED;
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && status == ReservationStatus.PENDING;
    }

    // Getters and Setters
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}