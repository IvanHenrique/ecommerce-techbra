package com.ecommerce.inventory.domain.model;

import com.ecommerce.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class Inventory extends BaseEntity {

    @Column(name = "product_id", unique = true, nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InventoryStatus status;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InventoryReservation> reservations = new ArrayList<>();

    protected Inventory() {
        // JPA Constructor
    }

    public Inventory(UUID productId, String productName, Integer initialQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = initialQuantity;
        this.reservedQuantity = 0;
        this.status = initialQuantity > 0 ? InventoryStatus.AVAILABLE : InventoryStatus.OUT_OF_STOCK;
    }

    public boolean canReserve(Integer quantity) {
        return availableQuantity >= quantity && quantity > 0;
    }

    public void reserve(Integer quantity, UUID orderId, String reservationReference) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient inventory to reserve " + quantity + " units");
        }

        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        
        var reservation = new InventoryReservation(orderId, quantity, reservationReference);
        reservation.setInventory(this);
        this.reservations.add(reservation);
        
        updateStatus();
    }

    public void releaseReservation(UUID orderId, Integer quantity) {
        var reservation = reservations.stream()
            .filter(r -> r.getOrderId().equals(orderId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No reservation found for order: " + orderId));

        reservation.cancel();
        this.availableQuantity += quantity;
        this.reservedQuantity -= quantity;
        
        updateStatus();
    }

    public void confirmReservation(UUID orderId) {
        var reservation = reservations.stream()
            .filter(r -> r.getOrderId().equals(orderId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No reservation found for order: " + orderId));

        reservation.confirm();
        // Reserved quantity becomes sold, so we reduce it from reserved
        this.reservedQuantity -= reservation.getQuantity();
        
        updateStatus();
    }

    private void updateStatus() {
        if (availableQuantity == 0 && reservedQuantity == 0) {
            this.status = InventoryStatus.OUT_OF_STOCK;
        } else if (availableQuantity == 0) {
            this.status = InventoryStatus.RESERVED;
        } else {
            this.status = InventoryStatus.AVAILABLE;
        }
    }

    // Getters
    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public List<InventoryReservation> getReservations() {
        return Collections.unmodifiableList(reservations);
    }

    public Integer getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }
}