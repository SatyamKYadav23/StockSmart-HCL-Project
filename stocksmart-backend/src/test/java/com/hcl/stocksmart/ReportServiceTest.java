package com.hcl.stocksmart;

import com.hcl.stocksmart.dto.report.*;
import com.hcl.stocksmart.entity.*;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.repository.*;
import com.hcl.stocksmart.services.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PurchaseReceivingRepository purchaseReceivingRepository;

    @Autowired
    private PurchaseReceivingItemRepository purchaseReceivingItemRepository;

    @Autowired
    private StockTransferRepository stockTransferRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private Product testProductA;
    private Product testProductNoCost;
    private Location testLocation1;
    private Location testLocation2;
    private Supplier testSupplier;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(Category.builder().name("Test Cat " + System.currentTimeMillis()).build())
        );

        testSupplier = supplierRepository.findAll().stream().findFirst().orElseGet(() ->
                supplierRepository.save(Supplier.builder()
                        .name("Test Supplier " + System.currentTimeMillis())
                        .phone("9876543210")
                        .email("test@supplier.com")
                        .address("123 Test Street")
                        .build())
        );

        List<Location> locations = locationRepository.findAll();
        if (locations.size() < 2) {
            testLocation1 = locationRepository.save(Location.builder().name("Facility Alpha").locationType("WAREHOUSE").build());
            testLocation2 = locationRepository.save(Location.builder().name("Facility Beta").locationType("RETAIL").build());
        } else {
            testLocation1 = locations.get(0);
            testLocation2 = locations.get(1);
        }

        // Product with cost price
        testProductA = productRepository.save(Product.builder()
                .sku("SKU-REP-A-" + System.currentTimeMillis())
                .name("Reporting Test Product A")
                .category(testCategory)
                .supplier(testSupplier)
                .price(new BigDecimal("1500.00"))
                .costPrice(new BigDecimal("1000.00"))
                .reorderLevel(15)
                .active(true)
                .build());

        // Product with NO cost price
        testProductNoCost = productRepository.save(Product.builder()
                .sku("SKU-REP-NOCOST-" + System.currentTimeMillis())
                .name("Reporting Test Product No Cost")
                .category(testCategory)
                .supplier(testSupplier)
                .price(new BigDecimal("2000.00"))
                .costPrice(null)
                .reorderLevel(10)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("1 & 2. Inventory Overview: Aggregation and available stock formula (available = onHand - reserved)")
    void testInventoryOverviewAggregationAndAvailableStock() {
        // Setup inventory
        inventoryRepository.save(Inventory.builder()
                .product(testProductA)
                .location(testLocation1)
                .quantity(50)
                .reservedQuantity(10)
                .build());

        inventoryRepository.save(Inventory.builder()
                .product(testProductNoCost)
                .location(testLocation1)
                .quantity(20)
                .reservedQuantity(5)
                .build());

        InventoryOverviewReportResponse overview = reportService.getInventoryOverview();

        assertTrue(overview.totalActiveProducts() >= 2);
        assertTrue(overview.totalUnitsOnHand() >= 70);
        assertTrue(overview.totalReservedUnits() >= 15);
        assertEquals(overview.totalUnitsOnHand() - overview.totalReservedUnits(), overview.totalAvailableUnits());
        assertTrue(overview.totalFacilities() >= 2);
        assertNotNull(overview.totalValuation());
        assertTrue(overview.totalValuation().compareTo(BigDecimal.ZERO) > 0);

        // Facility summaries
        assertFalse(overview.facilitySummaries().isEmpty());
        FacilityInventorySummaryDto loc1Summary = overview.facilitySummaries().stream()
                .filter(s -> s.locationId().equals(testLocation1.getId()))
                .findFirst().orElseThrow();

        assertEquals(loc1Summary.unitsOnHand() - loc1Summary.reservedUnits(), loc1Summary.availableUnits());
        assertTrue(loc1Summary.productsStocked() >= 2);
    }

    @Test
    @DisplayName("3 & 4. Stock Status: Reorder level classification (IN_STOCK, LOW_STOCK, OUT_OF_STOCK)")
    void testStockStatusClassification() {
        // 1. IN_STOCK item: qty 50 > reorderLevel 15
        inventoryRepository.save(Inventory.builder()
                .product(testProductA)
                .location(testLocation1)
                .quantity(50)
                .reservedQuantity(5)
                .build());

        // 2. LOW_STOCK item: qty 10 <= reorderLevel 15
        Product lowProd = productRepository.save(Product.builder()
                .sku("SKU-LOW-" + System.currentTimeMillis())
                .name("Low Stock Prod")
                .category(testCategory)
                .price(new BigDecimal("500.00"))
                .costPrice(new BigDecimal("300.00"))
                .reorderLevel(15)
                .active(true)
                .build());
        inventoryRepository.save(Inventory.builder()
                .product(lowProd)
                .location(testLocation1)
                .quantity(10)
                .reservedQuantity(0)
                .build());

        // 3. OUT_OF_STOCK item: qty 0
        Product oosProd = productRepository.save(Product.builder()
                .sku("SKU-OOS-" + System.currentTimeMillis())
                .name("OOS Prod")
                .category(testCategory)
                .price(new BigDecimal("500.00"))
                .costPrice(new BigDecimal("300.00"))
                .reorderLevel(5)
                .active(true)
                .build());
        inventoryRepository.save(Inventory.builder()
                .product(oosProd)
                .location(testLocation1)
                .quantity(0)
                .reservedQuantity(0)
                .build());

        // Test filter ALL
        StockStatusReportResponse report = reportService.getStockStatus(testLocation1.getId(), null, null, "ALL");
        assertTrue(report.inStockCount() >= 1);
        assertTrue(report.lowStockCount() >= 1);
        assertTrue(report.outOfStockCount() >= 1);

        // Verify status classification per item
        StockStatusItemDto inStockItem = report.items().stream()
                .filter(i -> i.productId().equals(testProductA.getId()) && i.locationId().equals(testLocation1.getId()))
                .findFirst().orElseThrow();
        assertEquals("IN_STOCK", inStockItem.status());
        assertEquals(45, inStockItem.available());

        StockStatusItemDto lowStockItem = report.items().stream()
                .filter(i -> i.productId().equals(lowProd.getId()) && i.locationId().equals(testLocation1.getId()))
                .findFirst().orElseThrow();
        assertEquals("LOW_STOCK", lowStockItem.status());

        StockStatusItemDto oosItem = report.items().stream()
                .filter(i -> i.productId().equals(oosProd.getId()) && i.locationId().equals(testLocation1.getId()))
                .findFirst().orElseThrow();
        assertEquals("OUT_OF_STOCK", oosItem.status());

        // Test filter by specific status
        StockStatusReportResponse filteredReport = reportService.getStockStatus(testLocation1.getId(), null, null, "LOW_STOCK");
        assertTrue(filteredReport.items().stream().allMatch(i -> "LOW_STOCK".equals(i.status())));
    }

    @Test
    @DisplayName("5 & 6. Inventory Valuation: Cost price basis and missing cost-price surfacing")
    void testInventoryValuationAndMissingCostSurfacing() {
        // Product A has costPrice 1000.00, qty 30 -> valuation 30,000.00
        inventoryRepository.save(Inventory.builder()
                .product(testProductA)
                .location(testLocation1)
                .quantity(30)
                .reservedQuantity(0)
                .build());

        // Product No Cost has null costPrice, qty 20 -> valuation 0.00, missingCost = true
        inventoryRepository.save(Inventory.builder()
                .product(testProductNoCost)
                .location(testLocation1)
                .quantity(20)
                .reservedQuantity(0)
                .build());

        InventoryValuationReportResponse valReport = reportService.getInventoryValuation(testLocation1.getId(), null);

        assertNotNull(valReport.totalValuation());
        assertTrue(valReport.totalValuation().compareTo(new BigDecimal("30000.00")) >= 0);
        assertTrue(valReport.productsWithMissingCost() >= 1);

        // Verify item level missing cost flag
        ProductValuationDto noCostItem = valReport.items().stream()
                .filter(i -> i.productId().equals(testProductNoCost.getId()))
                .findFirst().orElseThrow();
        assertTrue(noCostItem.missingCost());
        assertEquals(BigDecimal.ZERO, noCostItem.valuation());

        ProductValuationDto prodAItem = valReport.items().stream()
                .filter(i -> i.productId().equals(testProductA.getId()) && i.locationId().equals(testLocation1.getId()))
                .findFirst().orElseThrow();
        assertFalse(prodAItem.missingCost());
        assertEquals(new BigDecimal("30000.00"), prodAItem.valuation());
    }

    @Test
    @DisplayName("7 & 10. Stock Movements: Correct signed directions (Inflow vs Outflow) and net change")
    void testStockMovementsSignedQuantitiesAndNetChange() {
        LocalDateTime now = LocalDateTime.now();

        // RECEIPT: +25 units Inflow
        stockMovementRepository.save(StockMovement.builder()
                .product(testProductA)
                .location(testLocation1)
                .movementType("RECEIPT")
                .quantity(25)
                .referenceType("REC-001")
                .remarks("Purchase Receipt")
                .build());

        // SALE: -10 units Outflow
        stockMovementRepository.save(StockMovement.builder()
                .product(testProductA)
                .location(testLocation1)
                .movementType("SALE")
                .quantity(10)
                .referenceType("ORD-001")
                .remarks("Customer Order Fulfillment")
                .build());

        // TRANSFER OUT: -5 units Outflow
        stockMovementRepository.save(StockMovement.builder()
                .product(testProductA)
                .location(testLocation1)
                .movementType("TRANSFER")
                .quantity(5)
                .referenceType("TRF-001")
                .remarks("Transfer Out to " + testLocation2.getName())
                .build());

        // TRANSFER IN: +5 units Inflow
        stockMovementRepository.save(StockMovement.builder()
                .product(testProductA)
                .location(testLocation2)
                .movementType("TRANSFER")
                .quantity(5)
                .referenceType("TRF-001")
                .remarks("Transfer In from " + testLocation1.getName())
                .build());

        StockMovementReportResponse movementReport = reportService.getStockMovements(
                now.minusMinutes(5),
                now.plusMinutes(5),
                testProductA.getId(),
                null,
                null,
                null
        );

        assertEquals(4, movementReport.totalMovements());
        assertEquals(30, movementReport.totalInflowUnits()); // 25 (receipt) + 5 (transfer in)
        assertEquals(15, movementReport.totalOutflowUnits()); // 10 (sale) + 5 (transfer out)
        assertEquals(15, movementReport.netDeltaUnits());      // 30 - 15 = 15 net change
    }

    @Test
    @DisplayName("8 & 11. Order Summary: Aggregates status and separates completed sales revenue from pending")
    void testOrderSummaryAggregationAndRevenueSeparation() {
        LocalDateTime now = LocalDateTime.now();
        Location dedicatedOrderLoc = locationRepository.save(Location.builder()
                .name("Facility Ord Test " + System.currentTimeMillis())
                .locationType("RETAIL")
                .build());

        // 1. Completed order: 5 items, amount 5000.00
        Order compOrder = orderRepository.save(Order.builder()
                .orderNumber("ORD-COMP-" + System.currentTimeMillis())
                .status("COMPLETED")
                .customerName("Retail Customer A")
                .location(dedicatedOrderLoc)
                .totalAmount(new BigDecimal("5000.00"))
                .orderDate(now)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .order(compOrder)
                .product(testProductA)
                .quantity(5)
                .unitPrice(new BigDecimal("1000.00"))
                .subtotal(new BigDecimal("5000.00"))
                .build());

        // 2. Pending order: 2 items, amount 2000.00
        Order pendOrder = orderRepository.save(Order.builder()
                .orderNumber("ORD-PEND-" + System.currentTimeMillis())
                .status("PENDING")
                .customerName("Retail Customer B")
                .location(dedicatedOrderLoc)
                .totalAmount(new BigDecimal("2000.00"))
                .orderDate(now)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .order(pendOrder)
                .product(testProductA)
                .quantity(2)
                .unitPrice(new BigDecimal("1000.00"))
                .subtotal(new BigDecimal("2000.00"))
                .build());

        // 3. Cancelled order: 1 item, amount 1000.00
        Order cancOrder = orderRepository.save(Order.builder()
                .orderNumber("ORD-CANC-" + System.currentTimeMillis())
                .status("CANCELLED")
                .customerName("Retail Customer C")
                .location(dedicatedOrderLoc)
                .totalAmount(new BigDecimal("1000.00"))
                .orderDate(now)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .order(cancOrder)
                .product(testProductA)
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .subtotal(new BigDecimal("1000.00"))
                .build());

        OrderSummaryReportResponse report = reportService.getOrderSummary(
                now.minusMinutes(5),
                now.plusMinutes(5),
                "ALL",
                dedicatedOrderLoc.getId()
        );

        assertEquals(3, report.totalOrders());
        assertEquals(1, report.completedOrders());
        assertEquals(1, report.pendingOrders());
        assertEquals(1, report.cancelledOrders());
        assertEquals(8, report.totalQuantity());
        assertEquals(new BigDecimal("5000.00"), report.completedOrderValue(), "Only COMPLETED order amounts count toward realized sales value");
        assertEquals(new BigDecimal("8000.00"), report.totalOrderValue());
    }

    @Test
    @DisplayName("8. Purchase Receiving: Cancelled receipts are strictly excluded from received totals")
    void testPurchaseReceivingExcludesCancelledReceipts() {
        LocalDateTime now = LocalDateTime.now();
        Supplier dedicatedSupplier = supplierRepository.save(Supplier.builder()
                .name("Supplier PR Test " + System.currentTimeMillis())
                .phone("1112223333")
                .email("pr@test.com")
                .address("PR Test St")
                .build());

        // 1. RECEIVED receipt: 40 units, amount 40,000.00
        PurchaseReceiving rec1 = purchaseReceivingRepository.save(PurchaseReceiving.builder()
                .receivingNumber("REC-OK-" + System.currentTimeMillis())
                .receivingDate(now)
                .supplier(dedicatedSupplier)
                .location(testLocation1)
                .status("RECEIVED")
                .totalAmount(new BigDecimal("40000.00"))
                .build());
        purchaseReceivingItemRepository.save(PurchaseReceivingItem.builder()
                .receiving(rec1)
                .product(testProductA)
                .quantity(40)
                .unitCost(new BigDecimal("1000.00"))
                .subtotal(new BigDecimal("40000.00"))
                .build());

        // 2. CANCELLED receipt: 25 units, amount 25,000.00
        PurchaseReceiving recCancelled = purchaseReceivingRepository.save(PurchaseReceiving.builder()
                .receivingNumber("REC-CANC-" + System.currentTimeMillis())
                .receivingDate(now)
                .supplier(dedicatedSupplier)
                .location(testLocation1)
                .status("CANCELLED")
                .totalAmount(new BigDecimal("25000.00"))
                .build());
        purchaseReceivingItemRepository.save(PurchaseReceivingItem.builder()
                .receiving(recCancelled)
                .product(testProductA)
                .quantity(25)
                .unitCost(new BigDecimal("1000.00"))
                .subtotal(new BigDecimal("25000.00"))
                .build());

        PurchaseReceivingReportResponse report = reportService.getPurchaseReceivingReport(
                now.minusMinutes(5),
                now.plusMinutes(5),
                dedicatedSupplier.getId(),
                null,
                "ALL"
        );

        assertEquals(2, report.totalReceipts());
        assertEquals(1, report.completedReceipts());
        assertEquals(1, report.cancelledReceipts());
        // CANCELLED units and value MUST NOT be included
        assertEquals(40, report.receivedQuantity());
        assertEquals(new BigDecimal("40000.00"), report.receivedValue());
    }

    @Test
    @DisplayName("9 & 10. Stock Transfers: Cancelled transfers excluded and no double-counting")
    void testStockTransfersExcludeCancelledAndAvoidDoubleCounting() {
        LocalDateTime now = LocalDateTime.now();

        // 1. COMPLETED transfer: 15 units
        stockTransferRepository.save(StockTransfer.builder()
                .transferNumber("TRF-COMP-" + System.currentTimeMillis())
                .transferDate(now)
                .product(testProductA)
                .sourceLocation(testLocation1)
                .destinationLocation(testLocation2)
                .quantity(15)
                .status(StockTransferStatus.COMPLETED)
                .build());

        // 2. CANCELLED transfer: 10 units
        stockTransferRepository.save(StockTransfer.builder()
                .transferNumber("TRF-CANC-" + System.currentTimeMillis())
                .transferDate(now)
                .product(testProductA)
                .sourceLocation(testLocation1)
                .destinationLocation(testLocation2)
                .quantity(10)
                .status(StockTransferStatus.CANCELLED)
                .build());

        StockTransferReportResponse report = reportService.getStockTransferReport(
                now.minusMinutes(5),
                now.plusMinutes(5),
                testProductA.getId(),
                null,
                null,
                "ALL"
        );

        assertEquals(2, report.totalTransfers());
        assertEquals(1, report.completedTransfers());
        assertEquals(1, report.cancelledTransfers());
        // CANCELLED units MUST NOT be counted as relocated stock
        assertEquals(15, report.totalUnitsRelocated(), "Only COMPLETED transfers count toward stock relocated");

        // Source and destination breakdown
        FacilityTransferSummaryDto sourceSummary = report.sourceBreakdown().stream()
                .filter(s -> s.locationId().equals(testLocation1.getId()))
                .findFirst().orElseThrow();
        assertEquals(15, sourceSummary.units());
        assertEquals(1, sourceSummary.transferCount());
    }

    @Test
    @DisplayName("12 & 13. Edge Cases: Invalid date ranges reject with exception and empty queries execute cleanly")
    void testDateRangeValidationAndEmptyDatasetHandling() {
        LocalDateTime now = LocalDateTime.now();

        // Start date after end date must throw InvalidStockOperationException
        assertThrows(InvalidStockOperationException.class, () ->
                reportService.getOrderSummary(now.plusDays(1), now, "ALL", null)
        );

        assertThrows(InvalidStockOperationException.class, () ->
                reportService.getStockMovements(now.plusDays(1), now, null, null, null, null)
        );

        assertThrows(InvalidStockOperationException.class, () ->
                reportService.getPurchaseReceivingReport(now.plusDays(1), now, null, null, null)
        );

        assertThrows(InvalidStockOperationException.class, () ->
                reportService.getStockTransferReport(now.plusDays(1), now, null, null, null, null)
        );

        // Far-future date range should return empty results without crashing
        LocalDateTime futureStart = now.plusYears(50);
        LocalDateTime futureEnd = futureStart.plusDays(1);

        OrderSummaryReportResponse emptyOrders = reportService.getOrderSummary(futureStart, futureEnd, "ALL", null);
        assertEquals(0, emptyOrders.totalOrders());
        assertTrue(emptyOrders.items().isEmpty());

        StockMovementReportResponse emptyMovements = reportService.getStockMovements(futureStart, futureEnd, null, null, null, null);
        assertEquals(0, emptyMovements.totalMovements());
        assertEquals(0, emptyMovements.netDeltaUnits());

        PurchaseReceivingReportResponse emptyReceivings = reportService.getPurchaseReceivingReport(futureStart, futureEnd, null, null, null);
        assertEquals(0, emptyReceivings.totalReceipts());
        assertEquals(0, emptyReceivings.receivedQuantity());

        StockTransferReportResponse emptyTransfers = reportService.getStockTransferReport(futureStart, futureEnd, null, null, null, null);
        assertEquals(0, emptyTransfers.totalTransfers());
        assertEquals(0, emptyTransfers.totalUnitsRelocated());
    }
}
