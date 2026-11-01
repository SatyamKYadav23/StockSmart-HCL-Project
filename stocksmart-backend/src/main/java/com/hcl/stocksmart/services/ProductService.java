package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.ProductRequest;
import com.hcl.stocksmart.dto.ProductResponse;
import com.hcl.stocksmart.entity.Category;
import com.hcl.stocksmart.entity.Product;
import com.hcl.stocksmart.entity.Supplier;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.CategoryRepository;
import com.hcl.stocksmart.repository.InventoryRepository;
import com.hcl.stocksmart.repository.ProductRepository;
import com.hcl.stocksmart.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException(
                    "Product already exists with SKU: " + request.sku()
            );
        }

        if (request.barcode() != null &&
                productRepository.existsByBarcode(request.barcode())) {

            throw new DuplicateResourceException(
                    "Product already exists with barcode: " + request.barcode()
            );
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + request.categoryId()
                        )
                );

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Supplier not found with id: " + request.supplierId()
                            )
                    );
        }

        Product product = Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .category(category)
                .supplier(supplier)
                .price(request.price())
                .costPrice(request.costPrice())
                .barcode(request.barcode())
                .reorderLevel(
                        request.reorderLevel() != null
                                ? request.reorderLevel()
                                : 0
                )
                .active(true)
                .build();

        return mapToResponse(productRepository.save(product));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException(
                    "Product already exists with SKU: " + request.sku()
            );
        }

        if (request.barcode() != null && !request.barcode().equals(product.getBarcode())
                && productRepository.existsByBarcode(request.barcode())) {
            throw new DuplicateResourceException(
                    "Product already exists with barcode: " + request.barcode()
            );
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + request.categoryId()
                        )
                );

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Supplier not found with id: " + request.supplierId()
                            )
                    );
        }

        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setPrice(request.price());
        product.setCostPrice(request.costPrice());
        product.setBarcode(request.barcode());
        product.setReorderLevel(
                request.reorderLevel() != null
                        ? request.reorderLevel()
                        : 0
        );

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        if (inventoryRepository.existsByProductId(id)) {
            throw new InvalidStockOperationException(
                    "Cannot delete product '" + product.getName() + "' because it has associated inventory records."
            );
        }

        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSupplier() != null ? product.getSupplier().getId() : null,
                product.getSupplier() != null ? product.getSupplier().getName() : null,
                product.getPrice(),
                product.getCostPrice(),
                product.getBarcode(),
                product.getReorderLevel(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
