package com.hcl.stocksmart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(

        Long id,
        String sku,
        String name,
        String description,

        Long categoryId,
        String categoryName,

        Long supplierId,
        String supplierName,

        BigDecimal price,
        BigDecimal costPrice,
        String barcode,
        Integer reorderLevel,
        Boolean active,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}