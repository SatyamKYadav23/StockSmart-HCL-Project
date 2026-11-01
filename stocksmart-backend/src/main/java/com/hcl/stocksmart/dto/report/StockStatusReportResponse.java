package com.hcl.stocksmart.dto.report;

import java.util.List;

public record StockStatusReportResponse(
        int totalItems,
        int inStockCount,
        int lowStockCount,
        int outOfStockCount,
        int uninitializedCount,
        List<StockStatusItemDto> items
) {}
