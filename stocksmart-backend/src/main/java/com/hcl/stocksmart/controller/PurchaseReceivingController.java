package com.hcl.stocksmart.controller;

import com.hcl.stocksmart.dto.PurchaseReceivingRequest;
import com.hcl.stocksmart.dto.PurchaseReceivingResponse;
import com.hcl.stocksmart.services.PurchaseReceivingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-receivings")
@RequiredArgsConstructor
public class PurchaseReceivingController {

    private final PurchaseReceivingService purchaseReceivingService;

    @PostMapping
    public ResponseEntity<PurchaseReceivingResponse> createReceiving(
            @Valid @RequestBody PurchaseReceivingRequest request) {
        PurchaseReceivingResponse response = purchaseReceivingService.createReceiving(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseReceivingResponse>> getAllReceivings() {
        return ResponseEntity.ok(purchaseReceivingService.getAllReceivings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseReceivingResponse> getReceivingById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseReceivingService.getReceivingById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReceiving(@PathVariable Long id) {
        purchaseReceivingService.deleteReceiving(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseReceivingResponse> updateReceiving(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseReceivingRequest request) {
        PurchaseReceivingResponse response = purchaseReceivingService.updateReceiving(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PurchaseReceivingResponse> cancelReceiving(@PathVariable Long id) {
        PurchaseReceivingResponse response = purchaseReceivingService.cancelReceiving(id);
        return ResponseEntity.ok(response);
    }
}
