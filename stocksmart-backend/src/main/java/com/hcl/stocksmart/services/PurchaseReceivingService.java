package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.*;
import com.hcl.stocksmart.entity.*;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseReceivingService {

    private final PurchaseReceivingRepository purchaseReceivingRepository;
    private final PurchaseReceivingItemRepository purchaseReceivingItemRepository;
    private final SupplierRepository supplierRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    /**
     * Atomically records a purchase receipt from a supplier:
     * 1. Validates duplicate receiving numbers (HTTP 409)
     * 2. Validates supplier, location, and products
     * 3. Calculates subtotals and grand total server-side
     * 4. Persists PurchaseReceiving and line items
     * 5. Increases on-hand inventory quantity (reservedQuantity unchanged)
     * 6. Generates StockMovement audit record with type RECEIPT
     */
    @Transactional
    public PurchaseReceivingResponse createReceiving(PurchaseReceivingRequest request) {

        // 1. Idempotency / Duplicate Check
        if (purchaseReceivingRepository.existsByReceivingNumber(request.receivingNumber().trim())) {
            throw new DuplicateResourceException(
                    "Purchase receiving already exists with number: " + request.receivingNumber()
            );
        }

        // 2. Validate Supplier
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with id: " + request.supplierId()
                ));

        // 3. Validate Location
        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Location not found with id: " + request.locationId()
                ));

        // 4. Validate Items
        if (request.items() == null || request.items().isEmpty()) {
            throw new InvalidStockOperationException("Receiving must contain at least one item");
        }

        // 5. Server-side total calculation
        BigDecimal serverTotal = BigDecimal.ZERO;
        for (PurchaseReceivingItemRequest itemReq : request.items()) {
            if (itemReq.quantity() == null || itemReq.quantity() <= 0) {
                throw new InvalidStockOperationException("Received quantity must be greater than zero");
            }
            if (itemReq.unitCost() == null || itemReq.unitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidStockOperationException("Unit cost cannot be negative");
            }
            BigDecimal subtotal = itemReq.unitCost().multiply(BigDecimal.valueOf(itemReq.quantity()));
            serverTotal = serverTotal.add(subtotal);
        }

        String status = (request.status() != null && !request.status().isBlank())
                ? request.status().toUpperCase()
                : "RECEIVED";

        // 6. Persist PurchaseReceiving Entity
        PurchaseReceiving receiving = PurchaseReceiving.builder()
                .receivingNumber(request.receivingNumber().trim())
                .receivingDate(request.receivingDate() != null ? request.receivingDate() : LocalDateTime.now())
                .supplier(supplier)
                .location(location)
                .status(status)
                .totalAmount(serverTotal)
                .remarks(request.remarks())
                .build();

        PurchaseReceiving savedReceiving = purchaseReceivingRepository.save(receiving);

        List<PurchaseReceivingItemResponse> itemResponses = new ArrayList<>();
        boolean isReceived = "RECEIVED".equalsIgnoreCase(status);

        // 7. Persist Items and Update Inventory + Movements
        for (PurchaseReceivingItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + itemReq.productId()
                    ));

            BigDecimal itemSubtotal = itemReq.unitCost().multiply(BigDecimal.valueOf(itemReq.quantity()));

            PurchaseReceivingItem item = PurchaseReceivingItem.builder()
                    .receiving(savedReceiving)
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitCost(itemReq.unitCost())
                    .subtotal(itemSubtotal)
                    .build();

            PurchaseReceivingItem savedItem = purchaseReceivingItemRepository.save(item);

            itemResponses.add(new PurchaseReceivingItemResponse(
                    savedItem.getId(),
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    savedItem.getQuantity(),
                    savedItem.getUnitCost(),
                    savedItem.getSubtotal()
            ));

            if (isReceived) {
                // 8. Increase Inventory Quantity (find existing or initialize)
                Inventory inventory = inventoryRepository.findByProductIdAndLocationIdWithLock(product.getId(), location.getId())
                        .orElseGet(() -> Inventory.builder()
                                .product(product)
                                .location(location)
                                .quantity(0)
                                .reservedQuantity(0)
                                .build());

                inventory.setQuantity(inventory.getQuantity() + itemReq.quantity());
                inventoryRepository.save(inventory);

                // 9. Create StockMovement Audit Record (RECEIPT)
                StockMovement movement = StockMovement.builder()
                        .product(product)
                        .location(location)
                        .movementType("RECEIPT")
                        .direction("IN")
                        .quantity(itemReq.quantity())
                        .referenceType(savedReceiving.getReceivingNumber())
                        .referenceId(savedReceiving.getId())
                        .remarks("Purchase Receipt: " + savedReceiving.getReceivingNumber()
                                + " (" + supplier.getName() + ")")
                        .build();

                stockMovementRepository.save(movement);
            }
        }

        return mapToResponse(savedReceiving, itemResponses);
    }

    public List<PurchaseReceivingResponse> getAllReceivings() {
        return purchaseReceivingRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::mapReceivingWithItems)
                .toList();
    }

    public PurchaseReceivingResponse getReceivingById(Long id) {
        PurchaseReceiving receiving = purchaseReceivingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Purchase receiving not found with id: " + id)
                );

        return mapReceivingWithItems(receiving);
    }

    @Transactional
    public void deleteReceiving(Long id) {
        PurchaseReceiving receiving = purchaseReceivingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Purchase receiving not found with id: " + id)
                );

        if ("RECEIVED".equalsIgnoreCase(receiving.getStatus())) {
            throw new InvalidStockOperationException(
                    "Received purchase receipts cannot be deleted because they affect inventory history."
            );
        }

        purchaseReceivingItemRepository.deleteByReceivingId(id);
        purchaseReceivingRepository.delete(receiving);
    }

    @Transactional
    public PurchaseReceivingResponse cancelReceiving(Long id) {
        PurchaseReceiving receiving = purchaseReceivingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Purchase receiving not found with id: " + id)
                );

        if ("RECEIVED".equalsIgnoreCase(receiving.getStatus())) {
            throw new InvalidStockOperationException(
                    "Received purchase receipts cannot be cancelled because they affect inventory history."
            );
        }

        receiving.setStatus("CANCELLED");
        PurchaseReceiving updated = purchaseReceivingRepository.save(receiving);
        return mapReceivingWithItems(updated);
    }

    @Transactional
    public PurchaseReceivingResponse updateReceiving(Long id, PurchaseReceivingRequest request) {
        PurchaseReceiving receiving = purchaseReceivingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Purchase receiving not found with id: " + id)
                );

        if ("RECEIVED".equalsIgnoreCase(receiving.getStatus())) {
            throw new InvalidStockOperationException(
                    "Received purchase receipts are immutable and cannot be modified."
            );
        }

        if (request.remarks() != null) {
            receiving.setRemarks(request.remarks());
        }
        if (request.status() != null && !request.status().isBlank()) {
            if ("RECEIVED".equalsIgnoreCase(request.status())) {
                throw new InvalidStockOperationException(
                        "Cannot transition a non-received receipt to RECEIVED via update. Use the intake workflow."
                );
            }
            receiving.setStatus(request.status().toUpperCase());
        }

        PurchaseReceiving updated = purchaseReceivingRepository.save(receiving);
        return mapReceivingWithItems(updated);
    }

    private PurchaseReceivingResponse mapReceivingWithItems(PurchaseReceiving receiving) {
        List<PurchaseReceivingItem> items = purchaseReceivingItemRepository.findByReceivingId(receiving.getId());
        List<PurchaseReceivingItemResponse> itemResponses = items.stream()
                .map(item -> new PurchaseReceivingItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getSku(),
                        item.getQuantity(),
                        item.getUnitCost(),
                        item.getSubtotal()
                ))
                .toList();

        return mapToResponse(receiving, itemResponses);
    }

    private PurchaseReceivingResponse mapToResponse(
            PurchaseReceiving receiving,
            List<PurchaseReceivingItemResponse> items) {

        return new PurchaseReceivingResponse(
                receiving.getId(),
                receiving.getReceivingNumber(),
                receiving.getReceivingDate(),
                receiving.getSupplier().getId(),
                receiving.getSupplier().getName(),
                receiving.getLocation().getId(),
                receiving.getLocation().getName(),
                receiving.getStatus(),
                receiving.getTotalAmount(),
                receiving.getRemarks(),
                receiving.getCreatedAt(),
                items
        );
    }
}
