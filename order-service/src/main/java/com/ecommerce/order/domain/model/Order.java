package com.ecommerce.order.domain.model;

import com.ecommerce.shared.domain.entity.BaseEntity;
import com.ecommerce.shared.domain.valueobject.Money;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currency.currencyCode", column = @Column(name = "currency"))
    })
    @Embedded
    private Money totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    protected Order() {
        // JPA Constructor
    }

    public Order(String orderNumber, UUID customerId, Money totalAmount) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
        this.orderDate = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order in status: " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void markAsProcessing() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be marked as processing");
        }
        this.status = OrderStatus.PROCESSING;
    }

    public void markAsDelivered() {
        if (status != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Only processing orders can be delivered");
        }
        this.status = OrderStatus.DELIVERED;
    }

    private void recalculateTotal() {
        var total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(Money.zero("USD"), Money::add);
        this.totalAmount = total;
    }

    // Getters
    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }
}