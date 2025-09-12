package com.ecommerce.order.adapter.out.persistence;

import com.ecommerce.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.orderDate DESC")
    List<Order> findByCustomerIdOrderByOrderDateDesc(@Param("customerId") UUID customerId);

    boolean existsByOrderNumber(String orderNumber);
}