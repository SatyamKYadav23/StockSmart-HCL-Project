package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;

public record FacilityReceivingSummaryDto(
        Long locationId,
        String locationName,
        int receiptsCount,
        int receivedUnits,
        BigDecimal totalAmount
) {}
