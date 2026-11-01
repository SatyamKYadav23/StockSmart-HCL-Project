package com.hcl.stocksmart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Location ID is required")
        Long locationId,

        @NotNull(message = "Quantity change is required")
        Integer quantityChange,

        @Size(max = 50)
        String referenceNumber,

        @Size(max = 255)
        String notes
) {
}
