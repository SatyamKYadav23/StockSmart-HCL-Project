package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    @Query("SELECT sm FROM StockMovement sm WHERE " +
           "(:startDate IS NULL OR sm.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR sm.createdAt <= :endDate) AND " +
           "(:productId IS NULL OR sm.product.id = :productId) AND " +
           "(:locationId IS NULL OR sm.location.id = :locationId) AND " +
           "(:movementType IS NULL OR UPPER(sm.movementType) = UPPER(:movementType)) AND " +
           "(:reference IS NULL OR LOWER(sm.referenceType) LIKE LOWER(CONCAT('%', :reference, '%')) OR LOWER(sm.remarks) LIKE LOWER(CONCAT('%', :reference, '%'))) " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findMovementsFiltered(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("productId") Long productId,
            @Param("locationId") Long locationId,
            @Param("movementType") String movementType,
            @Param("reference") String reference
    );
}