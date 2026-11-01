package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.PurchaseReceivingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseReceivingItemRepository extends JpaRepository<PurchaseReceivingItem, Long> {

    List<PurchaseReceivingItem> findByReceivingId(Long receivingId);

    List<PurchaseReceivingItem> findByReceivingIdIn(List<Long> receivingIds);

    void deleteByReceivingId(Long receivingId);
}
