package com.hcl.stocksmart.dto.report;

import java.util.List;

public record StockMovementReportResponse(
        int totalMovements,
        int totalInflowUnits,
        int totalOutflowUnits,
        int netDeltaUnits,
        List<StockMovementReportItemDto> items
) {}
