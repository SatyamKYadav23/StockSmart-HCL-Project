package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;

public record FacilityInventorySummaryDto(
        Long locationId,
        String locationName,
        String locationType,
        int productsStocked,
        int unitsOnHand,
        int reservedUnits,
        int availableUnits,
        BigDecimal valuation
) {}
