package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record InventoryOverviewReportResponse(
        int totalActiveProducts,
        int totalUnitsOnHand,
        int totalReservedUnits,
        int totalAvailableUnits,
        long totalFacilities,
        BigDecimal totalValuation,
        List<FacilityInventorySummaryDto> facilitySummaries
) {}
