package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.PurchaseReceiving;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PurchaseReceivingRepository extends JpaRepository<PurchaseReceiving, Long> {

    boolean existsByReceivingNumber(String receivingNumber);

    Optional<PurchaseReceiving> findByReceivingNumber(String receivingNumber);

    @Query("SELECT pr FROM PurchaseReceiving pr WHERE " +
           "(:startDate IS NULL OR pr.receivingDate >= :startDate) AND " +
           "(:endDate IS NULL OR pr.receivingDate <= :endDate) AND " +
           "(:supplierId IS NULL OR pr.supplier.id = :supplierId) AND " +
           "(:locationId IS NULL OR pr.location.id = :locationId) AND " +
           "(:status IS NULL OR UPPER(pr.status) = UPPER(:status)) " +
           "ORDER BY pr.receivingDate DESC")
    List<PurchaseReceiving> findReceivingsFiltered(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("supplierId") Long supplierId,
            @Param("locationId") Long locationId,
            @Param("status") String status
    );
}
