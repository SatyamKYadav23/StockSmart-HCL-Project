package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE " +
           "(:startDate IS NULL OR o.orderDate >= :startDate) AND " +
           "(:endDate IS NULL OR o.orderDate <= :endDate) AND " +
           "(:status IS NULL OR UPPER(o.status) = UPPER(:status)) AND " +
           "(:locationId IS NULL OR (o.location IS NOT NULL AND o.location.id = :locationId)) " +
           "ORDER BY o.orderDate DESC")
    List<Order> findOrdersFiltered(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status,
            @Param("locationId") Long locationId
    );
}