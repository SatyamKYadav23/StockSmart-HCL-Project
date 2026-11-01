package com.hcl.stocksmart.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "SKU is required")
        @Size(max = 100)
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 150)
        String name,

        String description,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        Long supplierId,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal costPrice,

        @Size(max = 100)
        String barcode,

        @Min(value = 0, message = "Reorder level cannot be negative")
        Integer reorderLevel
) {
}