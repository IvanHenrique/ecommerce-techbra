package com.ecommerce.order.domain.model;

import com.ecommerce.shared.domain.entity.BaseEntity;
import com.ecommerce.shared.domain.valueobject.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency", length = 3)
    private String currency;

    protected OrderItem() {
        // JPA Constructor
    }

    public OrderItem(UUID productId, String productName, Integer quantity, Money unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        setUnitPrice(unitPrice);
    }

    public Money getSubtotal() {
        return getUnitPrice().multiply(java.math.BigDecimal.valueOf(quantity));
    }

    // Getters and Setters
    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return Money.of(unitPrice, currency);
    }

    private void setUnitPrice(Money money) {
        this.unitPrice = money.amount();
        this.currency = money.getCurrencyCode();
    }
}