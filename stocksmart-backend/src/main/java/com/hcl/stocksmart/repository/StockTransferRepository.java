package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.hcl.stocksmart.entity.StockTransferStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    boolean existsByTransferNumber(String transferNumber);

    Optional<StockTransfer> findByTransferNumber(String transferNumber);

    List<StockTransfer> findByProductId(Long productId);

    List<StockTransfer> findBySourceLocationId(Long sourceLocationId);

    List<StockTransfer> findByDestinationLocationId(Long destinationLocationId);

    @Query("SELECT st FROM StockTransfer st WHERE " +
           "(:startDate IS NULL OR st.transferDate >= :startDate) AND " +
           "(:endDate IS NULL OR st.transferDate <= :endDate) AND " +
           "(:productId IS NULL OR st.product.id = :productId) AND " +
           "(:sourceLocationId IS NULL OR st.sourceLocation.id = :sourceLocationId) AND " +
           "(:destinationLocationId IS NULL OR st.destinationLocation.id = :destinationLocationId) AND " +
           "(:status IS NULL OR st.status = :status) " +
           "ORDER BY st.transferDate DESC")
    List<StockTransfer> findTransfersFiltered(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("productId") Long productId,
            @Param("sourceLocationId") Long sourceLocationId,
            @Param("destinationLocationId") Long destinationLocationId,
            @Param("status") StockTransferStatus status
    );
}
