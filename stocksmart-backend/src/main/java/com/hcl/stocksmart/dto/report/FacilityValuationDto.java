package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;

public record FacilityValuationDto(
        Long locationId,
        String locationName,
        int totalUnits,
        BigDecimal valuation,
        int missingCostCount
) {}
