package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseReceivingReportItemDto(
        Long id,
        String receivingNumber,
        LocalDateTime receivingDate,
        Long supplierId,
        String supplierName,
        Long locationId,
        String locationName,
        String status,
        int itemCount,
        int receivedQuantity,
        BigDecimal totalAmount,
        String remarks
) {}
