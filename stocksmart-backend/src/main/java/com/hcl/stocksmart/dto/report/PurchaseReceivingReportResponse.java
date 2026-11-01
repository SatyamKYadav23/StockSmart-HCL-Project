package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseReceivingReportResponse(
        int totalReceipts,
        int completedReceipts,
        int cancelledReceipts,
        int receivedQuantity,
        BigDecimal receivedValue,
        List<SupplierReceivingSummaryDto> supplierBreakdown,
        List<FacilityReceivingSummaryDto> facilityBreakdown,
        List<PurchaseReceivingReportItemDto> items
) {}
