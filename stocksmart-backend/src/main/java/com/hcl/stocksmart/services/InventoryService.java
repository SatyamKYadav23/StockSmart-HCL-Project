package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.InventoryRequest;
import com.hcl.stocksmart.dto.InventoryResponse;
import com.hcl.stocksmart.dto.StockAdjustmentRequest;
import com.hcl.stocksmart.entity.Inventory;
import com.hcl.stocksmart.entity.Location;
import com.hcl.stocksmart.entity.Product;
import com.hcl.stocksmart.entity.StockMovement;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.InventoryRepository;
import com.hcl.stocksmart.repository.LocationRepository;
import com.hcl.stocksmart.repository.ProductRepository;
import com.hcl.stocksmart.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final StockMovementRepository stockMovementRepository;

    // CREATE
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {

        if (inventoryRepository.existsByProductIdAndLocationId(request.productId(), request.locationId())) {
            throw new DuplicateResourceException("Inventory already exists for this product and location");
        }

        int reserved = request.reservedQuantity() != null ? request.reservedQuantity() : 0;
        if (request.quantity() < reserved) {
            throw new InvalidStockOperationException(
                    "Quantity (" + request.quantity() + ") cannot be less than reserved quantity (" + reserved + ")"
            );
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + request.locationId()));

        Inventory inventory = Inventory.builder()
                .product(product)
                .location(location)
                .quantity(request.quantity())
                .reservedQuantity(reserved)
                .build();

        return mapToResponse(inventoryRepository.save(inventory));
    }

    // GET ALL
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // GET BY ID
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        return mapToResponse(inventory);
    }

    // UPDATE
    @Transactional
    public InventoryResponse updateInventory(Long id, InventoryRequest request) {

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        int reserved = request.reservedQuantity() != null ? request.reservedQuantity() : 0;
        if (request.quantity() < reserved) {
            throw new InvalidStockOperationException(
                    "Quantity (" + request.quantity() + ") cannot be less than reserved quantity (" + reserved + ")"
            );
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + request.locationId()));

        inventory.setProduct(product);
        inventory.setLocation(location);
        inventory.setQuantity(request.quantity());
        inventory.setReservedQuantity(reserved);

        return mapToResponse(inventoryRepository.save(inventory));
    }

    // DELETE
    @Transactional
    public void deleteInventory(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        inventoryRepository.delete(inventory);
    }

    // ATOMIC STOCK ADJUSTMENT
    @Transactional
    public InventoryResponse adjustStock(StockAdjustmentRequest request) {

        if (request.quantityChange() == null || request.quantityChange() == 0) {
            throw new InvalidStockOperationException("Adjustment quantity cannot be zero");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + request.locationId()));

        Inventory inventory = inventoryRepository
                .findByProductIdAndLocationIdWithLock(request.productId(), request.locationId())
                .orElse(null);

        if (inventory == null) {
            if (request.quantityChange() <= 0) {
                throw new InvalidStockOperationException(
                        "Cannot decrease stock: No inventory record exists for product '" + product.getName()
                                + "' at facility '" + location.getName() + "'."
                );
            }
            inventory = Inventory.builder()
                    .product(product)
                    .location(location)
                    .quantity(0)
                    .reservedQuantity(0)
                    .build();
        }

        int newQuantity = inventory.getQuantity() + request.quantityChange();

        if (newQuantity < 0) {
            throw new InvalidStockOperationException(
                    "Insufficient stock: Current quantity is " + inventory.getQuantity()
                            + ", cannot adjust by " + request.quantityChange()
                            + " (resulting stock would be negative: " + newQuantity + ")"
            );
        }

        if (newQuantity < inventory.getReservedQuantity()) {
            throw new InvalidStockOperationException(
                    "Insufficient stock: Resulting quantity (" + newQuantity
                            + ") cannot be less than reserved quantity ("
                            + inventory.getReservedQuantity() + ")"
            );
        }

        // 1. Update Inventory
        inventory.setQuantity(newQuantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 2. Create StockMovement Audit Record
        String direction = request.quantityChange() > 0 ? "Increased" : "Decreased";
        String notePart = (request.notes() != null && !request.notes().isBlank())
                ? (" - " + request.notes().trim())
                : "";
        String remarks = String.format("Stock %s by %d units%s",
                direction.toLowerCase(),
                Math.abs(request.quantityChange()),
                notePart
        );
        if (remarks.length() > 255) {
            remarks = remarks.substring(0, 255);
        }

        String refNumber = (request.referenceNumber() != null && !request.referenceNumber().isBlank())
                ? request.referenceNumber().trim()
                : "ADJUSTMENT";
        if (refNumber.length() > 50) {
            refNumber = refNumber.substring(0, 50);
        }

        StockMovement movement = StockMovement.builder()
                .product(product)
                .location(location)
                .movementType("ADJUSTMENT")
                .direction(request.quantityChange() > 0 ? "IN" : "OUT")
                .quantity(Math.abs(request.quantityChange()))
                .referenceType(refNumber)
                .remarks(remarks)
                .build();

        stockMovementRepository.save(movement);

        return mapToResponse(savedInventory);
    }

    // ENTITY -> RESPONSE
    private InventoryResponse mapToResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getProduct().getName(),
                inventory.getLocation().getId(),
                inventory.getLocation().getName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getUpdatedAt()
        );
    }
}
