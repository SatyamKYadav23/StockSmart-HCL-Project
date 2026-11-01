package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;

public record ProductValuationDto(
        Long productId,
        String sku,
        String productName,
        Long categoryId,
        String categoryName,
        Long locationId,
        String locationName,
        int quantity,
        BigDecimal costPrice,
        BigDecimal valuation,
        boolean missingCost
) {}
