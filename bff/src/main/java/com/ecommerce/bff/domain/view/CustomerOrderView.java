package com.ecommerce.bff.domain.view;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerOrderView(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    BigDecimal totalAmount,
    String currency,
    String orderStatus,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime orderDate,
    PaymentInfoView payment,
    List<InventoryInfoView> inventory,
    String overallStatus
) {
    
    public static CustomerOrderView create(UUID orderId, String orderNumber, UUID customerId,
                                         BigDecimal totalAmount, String currency, String orderStatus,
                                         LocalDateTime orderDate, PaymentInfoView payment,
                                         List<InventoryInfoView> inventory) {
        String overallStatus = determineOverallStatus(orderStatus, payment, inventory);
        
        return new CustomerOrderView(
            orderId, orderNumber, customerId, totalAmount, currency,
            orderStatus, orderDate, payment, inventory, overallStatus
        );
    }
    
    private static String determineOverallStatus(String orderStatus, PaymentInfoView payment, 
                                               List<InventoryInfoView> inventory) {
        if (payment == null || "FAILED".equals(payment.status())) {
            return "PAYMENT_FAILED";
        }
        
        if ("COMPLETED".equals(payment.status()) && 
            inventory.stream().allMatch(i -> "RESERVED".equals(i.status()))) {
            return "CONFIRMED";
        }
        
        if ("PENDING".equals(payment.status())) {
            return "PROCESSING_PAYMENT";
        }
        
        return "PENDING";
    }
}