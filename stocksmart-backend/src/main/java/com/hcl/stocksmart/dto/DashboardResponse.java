package com.hcl.stocksmart.dto;

public record DashboardResponse(

        long totalProducts,

        long totalCategories,

        long totalSuppliers,

        long totalLocations,

        long totalInventoryItems,

        long totalOrders,

        long lowStockProducts
) {
}