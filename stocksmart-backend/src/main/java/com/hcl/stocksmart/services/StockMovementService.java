package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.StockMovementRequest;
import com.hcl.stocksmart.dto.StockMovementResponse;
import com.hcl.stocksmart.entity.Location;
import com.hcl.stocksmart.entity.Product;
import com.hcl.stocksmart.entity.StockMovement;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
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
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public StockMovementResponse createMovement(StockMovementRequest request) {

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + request.productId()
                        )
                );

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Location not found with id: " + request.locationId()
                        )
                );

        String direction = request.direction();
        if (direction != null && !direction.isBlank()) {
            direction = direction.toUpperCase().trim();
        } else {
            if ("SALE".equalsIgnoreCase(request.movementType())) {
                direction = "OUT";
            } else if ("RECEIPT".equalsIgnoreCase(request.movementType())) {
                direction = "IN";
            } else {
                direction = "IN";
            }
        }

        StockMovement movement = StockMovement.builder()
                .product(product)
                .location(location)
                .movementType(request.movementType())
                .direction(direction)
                .quantity(request.quantity())
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .remarks(request.remarks())
                .build();

        return mapToResponse(stockMovementRepository.save(movement));
    }

    public List<StockMovementResponse> getAllMovements() {
        return stockMovementRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public StockMovementResponse getMovementById(Long id) {
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock movement not found with id: " + id
                        )
                );

        return mapToResponse(movement);
    }

    @Transactional
    public StockMovementResponse updateMovement(Long id, StockMovementRequest request) {

        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock movement not found with id: " + id
                        )
                );

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + request.productId()
                        )
                );

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Location not found with id: " + request.locationId()
                        )
                );

        movement.setProduct(product);
        movement.setLocation(location);
        movement.setMovementType(request.movementType());
        if (request.direction() != null && !request.direction().isBlank()) {
            movement.setDirection(request.direction().toUpperCase().trim());
        }
        movement.setQuantity(request.quantity());
        movement.setReferenceType(request.referenceType());
        movement.setReferenceId(request.referenceId());
        movement.setRemarks(request.remarks());

        return mapToResponse(stockMovementRepository.save(movement));
    }

    @Transactional
    public void deleteMovement(Long id) {

        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock movement not found with id: " + id
                        )
                );

        stockMovementRepository.delete(movement);
    }

    private StockMovementResponse mapToResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getSku(),
                movement.getLocation().getId(),
                movement.getLocation().getName(),
                movement.getMovementType(),
                movement.getEffectiveDirection(),
                movement.getQuantity(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getRemarks(),
                movement.getCreatedAt()
        );
    }
}
