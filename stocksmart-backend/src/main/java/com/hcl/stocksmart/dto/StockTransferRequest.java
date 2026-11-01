package com.hcl.stocksmart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record StockTransferRequest(

        @NotBlank(message = "Transfer number is required")
        @Size(max = 50, message = "Transfer number cannot exceed 50 characters")
        String transferNumber,

        LocalDateTime transferDate,

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Source location ID is required")
        Long sourceLocationId,

        @NotNull(message = "Destination location ID is required")
        Long destinationLocationId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Transfer quantity must be at least 1")
        Integer quantity,

        String status,

        @Size(max = 255, message = "Remarks cannot exceed 255 characters")
        String remarks
) {
}
