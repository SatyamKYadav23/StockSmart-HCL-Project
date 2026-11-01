package com.hcl.stocksmart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseReceivingRequest(

        @NotBlank(message = "Receiving number is required")
        @Size(max = 50, message = "Receiving number cannot exceed 50 characters")
        String receivingNumber,

        LocalDateTime receivingDate,

        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        @NotNull(message = "Location ID is required")
        Long locationId,

        String status,

        BigDecimal totalAmount,

        @Size(max = 255, message = "Remarks cannot exceed 255 characters")
        String remarks,

        @NotEmpty(message = "Receiving must contain at least one item")
        @Valid
        List<PurchaseReceivingItemRequest> items
) {
}
