package com.hcl.stocksmart.dto;

import java.math.BigDecimal;

public record PurchaseReceivingItemResponse(

        Long id,
        Long productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal subtotal
) {
}
