package com.hcl.stocksmart.dto;

import java.time.LocalDateTime;

public record InventoryResponse(

        Long id,

        Long productId,
        String productName,

        Long locationId,
        String locationName,

        Integer quantity,
        Integer reservedQuantity,

        LocalDateTime updatedAt
) {
}