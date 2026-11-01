package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;

public record CategoryValuationDto(
        Long categoryId,
        String categoryName,
        int totalUnits,
        BigDecimal valuation
) {}
