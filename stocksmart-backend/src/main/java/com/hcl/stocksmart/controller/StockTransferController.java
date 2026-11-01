package com.hcl.stocksmart.controller;

import com.hcl.stocksmart.dto.StockTransferRequest;
import com.hcl.stocksmart.dto.StockTransferResponse;
import com.hcl.stocksmart.services.StockTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @PostMapping
    public ResponseEntity<StockTransferResponse> createTransfer(
            @Valid @RequestBody StockTransferRequest request) {
        StockTransferResponse response = stockTransferService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StockTransferResponse>> getAllTransfers() {
        return ResponseEntity.ok(stockTransferService.getAllTransfers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockTransferResponse> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(stockTransferService.getTransferById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable Long id) {
        stockTransferService.deleteTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<StockTransferResponse> cancelTransfer(@PathVariable Long id) {
        StockTransferResponse response = stockTransferService.cancelTransfer(id);
        return ResponseEntity.ok(response);
    }
}
