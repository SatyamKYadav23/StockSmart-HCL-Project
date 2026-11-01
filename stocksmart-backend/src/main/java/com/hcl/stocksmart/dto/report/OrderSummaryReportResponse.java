package com.hcl.stocksmart.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record OrderSummaryReportResponse(
        int totalOrders,
        int completedOrders,
        int pendingOrders,
        int cancelledOrders,
        int totalQuantity,
        BigDecimal completedOrderValue,
        BigDecimal totalOrderValue,
        List<OrderSummaryItemDto> items
) {}
