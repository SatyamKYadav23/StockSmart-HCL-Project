package com.hcl.stocksmart.dto.report;

import java.time.LocalDateTime;

public record StockTransferReportItemDto(
        Long id,
        String transferNumber,
        LocalDateTime transferDate,
        Long productId,
        String productName,
        String sku,
        Long sourceLocationId,
        String sourceLocationName,
        Long destinationLocationId,
        String destinationLocationName,
        int quantity,
        String status,
        String remarks
) {}
