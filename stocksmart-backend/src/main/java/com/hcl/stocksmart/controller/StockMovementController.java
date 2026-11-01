package com.hcl.stocksmart.controller;

import com.hcl.stocksmart.dto.StockMovementRequest;
import com.hcl.stocksmart.dto.StockMovementResponse;
import com.hcl.stocksmart.services.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;


    @PostMapping
    public ResponseEntity<StockMovementResponse> createMovement(
            @Valid @RequestBody StockMovementRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(stockMovementService.createMovement(request));
    }


    @GetMapping
    public ResponseEntity<List<StockMovementResponse>> getAllMovements() {

        return ResponseEntity.ok(
                stockMovementService.getAllMovements()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<StockMovementResponse> getMovementById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                stockMovementService.getMovementById(id)
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<StockMovementResponse> updateMovement(
            @PathVariable Long id,
            @Valid @RequestBody StockMovementRequest request) {

        return ResponseEntity.ok(
                stockMovementService.updateMovement(id, request)
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovement(
            @PathVariable Long id) {

        stockMovementService.deleteMovement(id);

        return ResponseEntity.noContent().build();
    }
}