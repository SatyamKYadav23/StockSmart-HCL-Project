package com.hcl.stocksmart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseReceivingResponse(

        Long id,
        String receivingNumber,
        LocalDateTime receivingDate,
        Long supplierId,
        String supplierName,
        Long locationId,
        String locationName,
        String status,
        BigDecimal totalAmount,
        String remarks,
        LocalDateTime createdAt,
        List<PurchaseReceivingItemResponse> items
) {
}
