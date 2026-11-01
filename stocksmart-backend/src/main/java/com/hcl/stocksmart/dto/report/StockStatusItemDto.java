package com.hcl.stocksmart.dto.report;

public record StockStatusItemDto(
        Long productId,
        String sku,
        String productName,
        Long categoryId,
        String categoryName,
        Long locationId,
        String locationName,
        int onHand,
        int reserved,
        int available,
        int reorderLevel,
        String status,
        boolean initialized
) {}
