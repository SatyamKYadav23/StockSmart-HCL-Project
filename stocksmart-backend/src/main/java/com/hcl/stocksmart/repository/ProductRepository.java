package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
        SELECT COUNT(p)
        FROM Product p
        WHERE p.active = true AND COALESCE((SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.id = p.id), 0) <= p.reorderLevel
        """)
    long countLowStockProducts();

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByCategoryId(Long categoryId);
}
