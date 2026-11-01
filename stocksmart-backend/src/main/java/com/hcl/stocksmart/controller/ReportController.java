package com.hcl.stocksmart.controller;

import com.hcl.stocksmart.dto.report.*;
import com.hcl.stocksmart.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory-overview")
    public ResponseEntity<InventoryOverviewReportResponse> getInventoryOverview() {
        return ResponseEntity.ok(reportService.getInventoryOverview());
    }

    @GetMapping("/stock-status")
    public ResponseEntity<StockStatusReportResponse> getStockStatus(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(reportService.getStockStatus(locationId, categoryId, productId, status));
    }

    @GetMapping("/inventory-valuation")
    public ResponseEntity<InventoryValuationReportResponse> getInventoryValuation(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(reportService.getInventoryValuation(locationId, categoryId));
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<StockMovementReportResponse> getStockMovements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String reference
    ) {
        return ResponseEntity.ok(reportService.getStockMovements(startDate, endDate, productId, locationId, movementType, reference));
    }

    @GetMapping("/order-summary")
    public ResponseEntity<OrderSummaryReportResponse> getOrderSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long locationId
    ) {
        return ResponseEntity.ok(reportService.getOrderSummary(startDate, endDate, status, locationId));
    }

    @GetMapping("/purchase-receivings")
    public ResponseEntity<PurchaseReceivingReportResponse> getPurchaseReceivings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(reportService.getPurchaseReceivingReport(startDate, endDate, supplierId, locationId, status));
    }

    @GetMapping("/stock-transfers")
    public ResponseEntity<StockTransferReportResponse> getStockTransfers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long sourceLocationId,
            @RequestParam(required = false) Long destinationLocationId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(reportService.getStockTransferReport(startDate, endDate, productId, sourceLocationId, destinationLocationId, status));
    }
}
