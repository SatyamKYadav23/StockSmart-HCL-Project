package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.StockTransferRequest;
import com.hcl.stocksmart.dto.StockTransferResponse;
import com.hcl.stocksmart.entity.*;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    /**
     * Atomically executes an inter-facility stock transfer:
     * 1. Validates transfer number uniqueness (HTTP 409 Conflict)
     * 2. Validates product, source location, and destination location
     * 3. Rejects identical source and destination locations
     * 4. Validates positive transfer quantity
     * 5. Validates source inventory and available stock (available = quantity - reservedQuantity)
     * 6. Deducts source inventory (reservedQuantity unchanged)
     * 7. Increases destination inventory (creates record if missing, reservedQuantity unchanged)
     * 8. Persists StockTransfer record
     * 9. Creates two StockMovement audit records (Transfer Out from source, Transfer In to destination)
     * 10. Commits inside one transaction or rolls back on any error
     */
    @Transactional
    public StockTransferResponse createTransfer(StockTransferRequest request) {

        // 1. Idempotency Check
        if (stockTransferRepository.existsByTransferNumber(request.transferNumber().trim())) {
            throw new DuplicateResourceException(
                    "Stock transfer already exists with number: " + request.transferNumber()
            );
        }

        // 2. Validate Product
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.productId()
                ));

        // 3. Validate Source Location
        Location sourceLocation = locationRepository.findById(request.sourceLocationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source location not found with id: " + request.sourceLocationId()
                ));

        // 4. Validate Destination Location
        Location destinationLocation = locationRepository.findById(request.destinationLocationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination location not found with id: " + request.destinationLocationId()
                ));

        // 5. Source and Destination must be different
        if (sourceLocation.getId().equals(destinationLocation.getId())) {
            throw new InvalidStockOperationException(
                    "Source and destination locations must be different"
            );
        }

        // 6. Validate Quantity
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidStockOperationException(
                    "Transfer quantity must be greater than zero"
            );
        }

        // 7. Parse and Validate Status
        StockTransferStatus transferStatus;
        if (request.status() == null || request.status().isBlank()) {
            transferStatus = StockTransferStatus.COMPLETED;
        } else {
            try {
                transferStatus = StockTransferStatus.valueOf(request.status().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidStockOperationException(
                        "Invalid transfer status: '" + request.status()
                                + "'. Supported statuses are COMPLETED, CANCELLED."
                );
            }
        }

        // 8. Handle CANCELLED creation: persist record without mutating inventory or creating movement records
        if (transferStatus == StockTransferStatus.CANCELLED) {
            StockTransfer cancelledTransfer = StockTransfer.builder()
                    .transferNumber(request.transferNumber().trim())
                    .transferDate(request.transferDate() != null ? request.transferDate() : LocalDateTime.now())
                    .product(product)
                    .sourceLocation(sourceLocation)
                    .destinationLocation(destinationLocation)
                    .quantity(request.quantity())
                    .status(transferStatus)
                    .remarks(request.remarks())
                    .build();

            StockTransfer savedTransfer = stockTransferRepository.save(cancelledTransfer);
            return mapToResponse(savedTransfer);
        }

        // 9. Handle COMPLETED status with strict concurrency protection & deadlock-free lock ordering
        Long srcLocId = sourceLocation.getId();
        Long destLocId = destinationLocation.getId();

        Inventory sourceInventory;
        Inventory destinationInventory;

        if (srcLocId < destLocId) {
            // Lock Source first, Destination second
            sourceInventory = inventoryRepository
                    .findByProductIdAndLocationIdWithLock(product.getId(), srcLocId)
                    .orElseThrow(() -> new InvalidStockOperationException(
                            "Source inventory record does not exist for product " + product.getName()
                                    + " at location " + sourceLocation.getName()
                    ));

            int availableStock = sourceInventory.getQuantity() - sourceInventory.getReservedQuantity();
            if (request.quantity() > availableStock) {
                throw new InvalidStockOperationException(
                        "Insufficient available stock at " + sourceLocation.getName()
                                + ". Available: " + availableStock
                                + " (Total: " + sourceInventory.getQuantity()
                                + ", Reserved: " + sourceInventory.getReservedQuantity()
                                + "), Requested: " + request.quantity()
                );
            }

            destinationInventory = inventoryRepository
                    .findByProductIdAndLocationIdWithLock(product.getId(), destLocId)
                    .orElseGet(() -> Inventory.builder()
                            .product(product)
                            .location(destinationLocation)
                            .quantity(0)
                            .reservedQuantity(0)
                            .build());
        } else {
            // destLocId < srcLocId: Lock Destination first, Source second
            destinationInventory = inventoryRepository
                    .findByProductIdAndLocationIdWithLock(product.getId(), destLocId)
                    .orElseGet(() -> Inventory.builder()
                            .product(product)
                            .location(destinationLocation)
                            .quantity(0)
                            .reservedQuantity(0)
                            .build());

            sourceInventory = inventoryRepository
                    .findByProductIdAndLocationIdWithLock(product.getId(), srcLocId)
                    .orElseThrow(() -> new InvalidStockOperationException(
                            "Source inventory record does not exist for product " + product.getName()
                                    + " at location " + sourceLocation.getName()
                    ));

            int availableStock = sourceInventory.getQuantity() - sourceInventory.getReservedQuantity();
            if (request.quantity() > availableStock) {
                throw new InvalidStockOperationException(
                        "Insufficient available stock at " + sourceLocation.getName()
                                + ". Available: " + availableStock
                                + " (Total: " + sourceInventory.getQuantity()
                                + ", Reserved: " + sourceInventory.getReservedQuantity()
                                + "), Requested: " + request.quantity()
                );
            }
        }

        // 10. Mutate Inventories
        sourceInventory.setQuantity(sourceInventory.getQuantity() - request.quantity());
        inventoryRepository.save(sourceInventory);

        destinationInventory.setQuantity(destinationInventory.getQuantity() + request.quantity());
        inventoryRepository.save(destinationInventory);

        // 11. Persist StockTransfer
        StockTransfer transfer = StockTransfer.builder()
                .transferNumber(request.transferNumber().trim())
                .transferDate(request.transferDate() != null ? request.transferDate() : LocalDateTime.now())
                .product(product)
                .sourceLocation(sourceLocation)
                .destinationLocation(destinationLocation)
                .quantity(request.quantity())
                .status(transferStatus)
                .remarks(request.remarks())
                .build();

        StockTransfer savedTransfer = stockTransferRepository.save(transfer);

        // 12. Create Dual StockMovement Audit Records
        // Source: Transfer Out
        StockMovement sourceMovement = StockMovement.builder()
                .product(product)
                .location(sourceLocation)
                .movementType("TRANSFER")
                .direction("OUT")
                .quantity(request.quantity())
                .referenceType(savedTransfer.getTransferNumber())
                .referenceId(savedTransfer.getId())
                .remarks("Transfer Out to " + destinationLocation.getName())
                .build();
        stockMovementRepository.save(sourceMovement);

        // Destination: Transfer In
        StockMovement destinationMovement = StockMovement.builder()
                .product(product)
                .location(destinationLocation)
                .movementType("TRANSFER")
                .direction("IN")
                .quantity(request.quantity())
                .referenceType(savedTransfer.getTransferNumber())
                .referenceId(savedTransfer.getId())
                .remarks("Transfer In from " + sourceLocation.getName())
                .build();
        stockMovementRepository.save(destinationMovement);

        return mapToResponse(savedTransfer);
    }

    public List<StockTransferResponse> getAllTransfers() {
        return stockTransferRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::mapToResponse)
                .toList();
    }

    public StockTransferResponse getTransferById(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock transfer not found with id: " + id
                ));
        return mapToResponse(transfer);
    }

    @Transactional
    public void deleteTransfer(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock transfer not found with id: " + id
                ));

        if (StockTransferStatus.COMPLETED.equals(transfer.getStatus())) {
            throw new InvalidStockOperationException(
                    "Completed stock transfers cannot be deleted because they affect inventory history."
            );
        }

        stockTransferRepository.delete(transfer);
    }

    @Transactional
    public StockTransferResponse cancelTransfer(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock transfer not found with id: " + id
                ));

        if (StockTransferStatus.COMPLETED.equals(transfer.getStatus())) {
            throw new InvalidStockOperationException(
                    "Completed stock transfers cannot be cancelled because they affect inventory history."
            );
        }

        transfer.setStatus(StockTransferStatus.CANCELLED);
        return mapToResponse(stockTransferRepository.save(transfer));
    }

    private StockTransferResponse mapToResponse(StockTransfer transfer) {
        return new StockTransferResponse(
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getTransferDate(),
                transfer.getProduct().getId(),
                transfer.getProduct().getName(),
                transfer.getProduct().getSku(),
                transfer.getSourceLocation().getId(),
                transfer.getSourceLocation().getName(),
                transfer.getDestinationLocation().getId(),
                transfer.getDestinationLocation().getName(),
                transfer.getQuantity(),
                transfer.getStatus().name(),
                transfer.getRemarks(),
                transfer.getCreatedAt()
        );
    }
}
