package com.hcl.stocksmart.dto.report;

import java.time.LocalDateTime;

public record StockMovementReportItemDto(
        Long id,
        LocalDateTime createdAt,
        String referenceType,
        Long referenceId,
        Long productId,
        String productName,
        String sku,
        Long locationId,
        String locationName,
        String movementType,
        String direction,
        int quantity,
        int signedQuantity,
        String remarks
) {}
