package com.hcl.stocksmart.dto.report;

import java.util.List;

public record StockTransferReportResponse(
        int totalTransfers,
        int completedTransfers,
        int cancelledTransfers,
        int totalUnitsRelocated,
        List<FacilityTransferSummaryDto> sourceBreakdown,
        List<FacilityTransferSummaryDto> destinationBreakdown,
        List<ProductTransferSummaryDto> productBreakdown,
        List<StockTransferReportItemDto> items
) {}
