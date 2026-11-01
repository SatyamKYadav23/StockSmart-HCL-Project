package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record InventoryValuationReportResponse(
        BigDecimal totalValuation,
        int totalUnitsOnHand,
        int productsWithMissingCost,
        List<FacilityValuationDto> facilityBreakdown,
        List<CategoryValuationDto> categoryBreakdown,
        List<ProductValuationDto> items
) {}
