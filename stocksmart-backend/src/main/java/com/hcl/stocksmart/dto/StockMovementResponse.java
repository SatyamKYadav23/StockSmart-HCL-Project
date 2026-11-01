package com.hcl.stocksmart.dto;

import java.time.LocalDateTime;

public record StockMovementResponse(

        Long id,

        Long productId,
        String productName,
        String productSku,

        Long locationId,
        String locationName,

        String movementType,
        String direction,
        Integer quantity,

        String referenceType,
        Long referenceId,

        String remarks,

        LocalDateTime createdAt
) {
}