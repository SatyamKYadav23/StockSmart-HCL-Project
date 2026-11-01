package com.hcl.stocksmart.dto.report;

public record ProductTransferSummaryDto(
        Long productId,
        String productName,
        String sku,
        int transferCount,
        int unitsRelocated
) {}
