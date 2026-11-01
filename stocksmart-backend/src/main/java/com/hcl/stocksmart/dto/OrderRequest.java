package com.hcl.stocksmart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(

        @NotBlank(message = "Order number is required")
        String orderNumber,

        @NotBlank(message = "Order status is required")
        String status,

        String customerName,

        Long locationId,

        BigDecimal totalAmount,

        String remarks,

        @Valid
        List<OrderItemRequest> items
) {
}