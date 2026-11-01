package com.hcl.stocksmart.dto;

import java.time.LocalDateTime;

public record StockTransferResponse(
        Long id,
        String transferNumber,
        LocalDateTime transferDate,
        Long productId,
        String productName,
        String productSku,
        Long sourceLocationId,
        String sourceLocationName,
        Long destinationLocationId,
        String destinationLocationName,
        Integer quantity,
        String status,
        String remarks,
        LocalDateTime createdAt
) {
}
