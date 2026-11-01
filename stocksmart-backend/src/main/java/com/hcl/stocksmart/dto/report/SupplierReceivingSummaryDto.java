package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;

public record SupplierReceivingSummaryDto(
        Long supplierId,
        String supplierName,
        int receiptsCount,
        int receivedUnits,
        BigDecimal totalAmount
) {}
