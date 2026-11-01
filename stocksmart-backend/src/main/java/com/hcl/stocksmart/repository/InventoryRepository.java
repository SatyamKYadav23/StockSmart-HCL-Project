package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductIdAndLocationId(
            Long productId,
            Long locationId
    );

    Optional<Inventory> findByProductIdAndLocationId(
            Long productId,
            Long locationId
    );

    java.util.List<Inventory> findByLocationId(Long locationId);

    boolean existsByLocationId(Long locationId);

    java.util.List<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.location.id = :locationId")
    Optional<Inventory> findByProductIdAndLocationIdWithLock(
            @Param("productId") Long productId,
            @Param("locationId") Long locationId
    );
}