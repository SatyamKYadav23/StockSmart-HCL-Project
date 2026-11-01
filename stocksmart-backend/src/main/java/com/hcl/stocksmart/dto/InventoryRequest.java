package com.hcl.stocksmart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Location ID is required")
        Long locationId,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity,

        @Min(value = 0, message = "Reserved quantity cannot be negative")
        Integer reservedQuantity
) {
}