package com.hcl.stocksmart.dto.report;

public record FacilityTransferSummaryDto(
        Long locationId,
        String locationName,
        int transferCount,
        int units
) {}
