package com.hcl.stocksmart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(

        Long id,

        String orderNumber,

        LocalDateTime orderDate,

        String status,

        String customerName,

        Long locationId,
        String locationName,

        BigDecimal totalAmount,

        String remarks,

        LocalDateTime createdAt,

        List<OrderItemResponse> items
) {
}