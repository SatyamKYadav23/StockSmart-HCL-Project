package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryItemDto(
        Long id,
        String orderNumber,
        LocalDateTime orderDate,
        String customerName,
        Long locationId,
        String locationName,
        String status,
        int itemCount,
        int totalQuantity,
        BigDecimal orderValue
) {}
