package com.hcl.stocksmart;

import com.hcl.stocksmart.dto.StockAdjustmentRequest;
import com.hcl.stocksmart.dto.StockTransferRequest;
import com.hcl.stocksmart.dto.StockTransferResponse;
import com.hcl.stocksmart.entity.Inventory;
import com.hcl.stocksmart.entity.Location;
import com.hcl.stocksmart.entity.Product;
import com.hcl.stocksmart.entity.StockMovement;
import com.hcl.stocksmart.entity.StockTransferStatus;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.InventoryRepository;
import com.hcl.stocksmart.repository.LocationRepository;
import com.hcl.stocksmart.repository.ProductRepository;
import com.hcl.stocksmart.repository.StockMovementRepository;
import com.hcl.stocksmart.repository.StockTransferRepository;
import com.hcl.stocksmart.services.InventoryService;
import com.hcl.stocksmart.services.StockTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockTransferTransactionTest {

    @Autowired
    private StockTransferService stockTransferService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StockTransferRepository stockTransferRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long testProductId;
    private Long sourceLocationId;
    private Long destinationLocationId;
    private Long sourceInventoryId;
    private Long destinationInventoryId;

    @BeforeEach
    void setUp() {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        testProductId = product.getId();

        List<Location> locations = locationRepository.findAll();
        assertTrue(locations.size() >= 2, "Test requires at least two locations in database");

        sourceLocationId = locations.get(0).getId();
        destinationLocationId = locations.get(1).getId();

        // Ensure source inventory has adequate quantity & reserved stock
        Location sourceLoc = locations.get(0);
        Inventory srcInv = inventoryRepository.findByProductIdAndLocationId(testProductId, sourceLocationId)
                .orElseGet(() -> {
                    Inventory inv = Inventory.builder()
                            .product(product)
                            .location(sourceLoc)
                            .quantity(40)
                            .reservedQuantity(5)
                            .build();
                    return inventoryRepository.save(inv);
                });

        if (srcInv.getQuantity() < 30) {
            srcInv.setQuantity(50);
            srcInv.setReservedQuantity(5);
            srcInv = inventoryRepository.save(srcInv);
        }
        sourceInventoryId = srcInv.getId();

        // Ensure destination inventory exists or gets tracked
        Location destLoc = locations.get(1);
        Inventory destInv = inventoryRepository.findByProductIdAndLocationId(testProductId, destinationLocationId)
                .orElseGet(() -> {
                    Inventory inv = Inventory.builder()
                            .product(product)
                            .location(destLoc)
                            .quantity(10)
                            .reservedQuantity(0)
                            .build();
                    return inventoryRepository.save(inv);
                });
        destinationInventoryId = destInv.getId();
    }

    @Test
    @DisplayName("Verify successful inter-facility transfer atomically updates source, destination, and dual movements")
    void testSuccessfulTransferWorkflow() {
        Inventory initialSrc = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        Inventory initialDest = inventoryRepository.findById(destinationInventoryId).orElseThrow();

        int initialSrcQty = initialSrc.getQuantity();
        int initialSrcReserved = initialSrc.getReservedQuantity();
        int initialDestQty = initialDest.getQuantity();
        int initialDestReserved = initialDest.getReservedQuantity();
        long initialMovements = stockMovementRepository.count();

        String transferNum = "TRF-TEST-" + System.currentTimeMillis();
        int transferQty = 10;

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                destinationLocationId,
                transferQty,
                "COMPLETED",
                "Phase 9 integration test transfer"
        );

        StockTransferResponse response = stockTransferService.createTransfer(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(transferNum, response.transferNumber());
        assertEquals(transferQty, response.quantity());
        assertEquals("COMPLETED", response.status());

        // Verify source inventory deducted and reserved untouched
        Inventory updatedSrc = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        assertEquals(initialSrcQty - transferQty, updatedSrc.getQuantity());
        assertEquals(initialSrcReserved, updatedSrc.getReservedQuantity());

        // Verify destination inventory incremented and reserved untouched
        Inventory updatedDest = inventoryRepository.findById(destinationInventoryId).orElseThrow();
        assertEquals(initialDestQty + transferQty, updatedDest.getQuantity());
        assertEquals(initialDestReserved, updatedDest.getReservedQuantity());

        // Verify total inventory across facilities remains invariant
        assertEquals(initialSrcQty + initialDestQty, updatedSrc.getQuantity() + updatedDest.getQuantity());

        // Verify dual StockMovement audit records created
        assertEquals(initialMovements + 2, stockMovementRepository.count());
        List<StockMovement> movements = stockMovementRepository.findAll();
        StockMovement destMovement = movements.get(movements.size() - 1);
        StockMovement srcMovement = movements.get(movements.size() - 2);

        assertEquals("TRANSFER", srcMovement.getMovementType());
        assertEquals(transferQty, srcMovement.getQuantity());
        assertEquals(sourceLocationId, srcMovement.getLocation().getId());
        assertEquals(transferNum, srcMovement.getReferenceType());
        assertTrue(srcMovement.getRemarks().contains("Transfer Out to"));

        assertEquals("TRANSFER", destMovement.getMovementType());
        assertEquals(transferQty, destMovement.getQuantity());
        assertEquals(destinationLocationId, destMovement.getLocation().getId());
        assertEquals(transferNum, destMovement.getReferenceType());
        assertTrue(destMovement.getRemarks().contains("Transfer In from"));
    }

    @Test
    @DisplayName("Verify duplicate transfer number is rejected with DuplicateResourceException (HTTP 409)")
    void testDuplicateTransferNumberRejection() {
        String transferNum = "TRF-DUP-" + System.currentTimeMillis();
        int transferQty = 5;

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                destinationLocationId,
                transferQty,
                "COMPLETED",
                "Initial transfer"
        );

        // First attempt succeeds
        StockTransferResponse response = stockTransferService.createTransfer(request);
        assertNotNull(response);

        Inventory srcAfterFirst = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        Inventory destAfterFirst = inventoryRepository.findById(destinationInventoryId).orElseThrow();
        int srcQtyAfterFirst = srcAfterFirst.getQuantity();
        int destQtyAfterFirst = destAfterFirst.getQuantity();
        long movementsAfterFirst = stockMovementRepository.count();

        // Second attempt with same transferNumber must throw DuplicateResourceException
        assertThrows(DuplicateResourceException.class, () -> {
            stockTransferService.createTransfer(request);
        });

        // Verify inventory and movements did NOT change after second request
        Inventory srcAfterSecond = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        Inventory destAfterSecond = inventoryRepository.findById(destinationInventoryId).orElseThrow();
        assertEquals(srcQtyAfterFirst, srcAfterSecond.getQuantity());
        assertEquals(destQtyAfterFirst, destAfterSecond.getQuantity());
        assertEquals(movementsAfterFirst, stockMovementRepository.count());
    }

    @Test
    @DisplayName("Verify transfer exceeding available stock (quantity - reserved) is rejected")
    void testInsufficientAvailableStockRejection() {
        Inventory srcInv = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        int totalQty = srcInv.getQuantity();
        int reservedQty = srcInv.getReservedQuantity();
        int availableQty = totalQty - reservedQty;

        long initialTransfers = stockTransferRepository.count();
        long initialMovements = stockMovementRepository.count();

        String transferNum = "TRF-EXCEED-" + System.currentTimeMillis();
        int excessiveQty = availableQty + 1; // 1 unit greater than available stock

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                destinationLocationId,
                excessiveQty,
                "COMPLETED",
                "Excessive stock request"
        );

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> stockTransferService.createTransfer(request)
        );
        assertTrue(ex.getMessage().contains("Insufficient available stock"));

        // Verify no changes persisted
        assertEquals(initialTransfers, stockTransferRepository.count());
        assertEquals(initialMovements, stockMovementRepository.count());
        Inventory srcAfter = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        assertEquals(totalQty, srcAfter.getQuantity());
        assertEquals(reservedQty, srcAfter.getReservedQuantity());
    }

    @Test
    @DisplayName("Verify transfer with identical source and destination is rejected")
    void testSameSourceAndDestinationRejection() {
        String transferNum = "TRF-SAME-LOC-" + System.currentTimeMillis();

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                sourceLocationId, // identical source and destination
                5,
                "COMPLETED",
                "Same location test"
        );

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> stockTransferService.createTransfer(request)
        );
        assertTrue(ex.getMessage().contains("Source and destination locations must be different"));
    }

    @Test
    @DisplayName("Verify transfer with invalid product is rejected")
    void testInvalidProductRejection() {
        String transferNum = "TRF-INV-PROD-" + System.currentTimeMillis();
        Long nonExistentProdId = 999999L;

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                nonExistentProdId,
                sourceLocationId,
                destinationLocationId,
                5,
                "COMPLETED",
                "Invalid product test"
        );

        assertThrows(ResourceNotFoundException.class, () -> {
            stockTransferService.createTransfer(request);
        });
    }

    @Test
    @DisplayName("Verify transfer with invalid destination location is rejected")
    void testInvalidLocationRejection() {
        String transferNum = "TRF-INV-LOC-" + System.currentTimeMillis();
        Long nonExistentLocId = 999999L;

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                nonExistentLocId,
                5,
                "COMPLETED",
                "Invalid destination test"
        );

        assertThrows(ResourceNotFoundException.class, () -> {
            stockTransferService.createTransfer(request);
        });
    }

    @Test
    @DisplayName("Verify atomic transaction rollback when an error occurs during transfer")
    void testControlledTransactionRollback() {
        Inventory initialSrc = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        Inventory initialDest = inventoryRepository.findById(destinationInventoryId).orElseThrow();
        int initialSrcQty = initialSrc.getQuantity();
        int initialDestQty = initialDest.getQuantity();
        long initialTransfers = stockTransferRepository.count();
        long initialMovements = stockMovementRepository.count();

        String transferNum = "TRF-ROLLBACK-" + System.currentTimeMillis();

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            StockTransferRequest request = new StockTransferRequest(
                    transferNum,
                    LocalDateTime.now(),
                    testProductId,
                    sourceLocationId,
                    destinationLocationId,
                    10,
                    "COMPLETED",
                    "Rollback test transfer"
            );

            stockTransferService.createTransfer(request);
            // Simulate abnormal error after execution
            throw new RuntimeException("Simulated unexpected failure during stock transfer workflow");
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
        }

        // Verify everything was cleanly rolled back
        assertEquals(initialTransfers, stockTransferRepository.count());
        assertEquals(initialMovements, stockMovementRepository.count());
        Inventory srcAfter = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        Inventory destAfter = inventoryRepository.findById(destinationInventoryId).orElseThrow();
        assertEquals(initialSrcQty, srcAfter.getQuantity());
        assertEquals(initialDestQty, destAfter.getQuantity());
    }

    @Test
    @DisplayName("Verify completed stock transfer cannot be deleted (HTTP 409)")
    void testCompletedTransferCannotBeDeleted() {
        String transferNum = "TRF-DEL-TEST-" + System.currentTimeMillis();
        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                destinationLocationId,
                4,
                "COMPLETED",
                "Transfer delete test"
        );

        StockTransferResponse created = stockTransferService.createTransfer(request);
        assertNotNull(created.id());

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> stockTransferService.deleteTransfer(created.id())
        );

        assertTrue(ex.getMessage().contains("Completed stock transfers cannot be deleted because they affect inventory history."));
        assertTrue(stockTransferRepository.findById(created.id()).isPresent());
    }

    @Test
    @DisplayName("Verify completed stock transfer cannot be cancelled (HTTP 409)")
    void testCompletedTransferCannotBeCancelled() {
        String transferNum = "TRF-CAN-TEST-" + System.currentTimeMillis();
        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                destinationLocationId,
                3,
                "COMPLETED",
                "Transfer cancel test"
        );

        StockTransferResponse created = stockTransferService.createTransfer(request);
        assertNotNull(created.id());

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> stockTransferService.cancelTransfer(created.id())
        );

        assertTrue(ex.getMessage().contains("Completed stock transfers cannot be cancelled because they affect inventory history."));
        assertEquals(StockTransferStatus.COMPLETED, stockTransferRepository.findById(created.id()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Verify transfer is rejected when source inventory record does not exist")
    void testMissingSourceInventoryRejection() {
        Product existing = productRepository.findById(testProductId).orElseThrow();
        Product unstockedProduct = productRepository.save(Product.builder()
                .name("Unstocked Transfer Source " + System.currentTimeMillis())
                .sku("UNSTOCKED-SRC-" + System.currentTimeMillis())
                .category(existing.getCategory())
                .price(java.math.BigDecimal.valueOf(129.99))
                .reorderLevel(5)
                .active(true)
                .build());

        String transferNum = "TRF-NO-SRC-" + System.currentTimeMillis();
        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                unstockedProduct.getId(),
                sourceLocationId,
                destinationLocationId,
                5,
                "COMPLETED",
                "Missing source inventory test"
        );

        long initialTransfers = stockTransferRepository.count();
        long initialMovements = stockMovementRepository.count();

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> stockTransferService.createTransfer(request)
        );
        assertTrue(ex.getMessage().contains("Source inventory record does not exist"));

        assertEquals(initialTransfers, stockTransferRepository.count());
        assertEquals(initialMovements, stockMovementRepository.count());
    }

    @Test
    @DisplayName("Verify destination inventory is initialized when destination record does not exist beforehand")
    void testDestinationInventoryInitialization() {
        Product existing = productRepository.findById(testProductId).orElseThrow();
        Product testProduct = productRepository.save(Product.builder()
                .name("Dest Init Product " + System.currentTimeMillis())
                .sku("DEST-INIT-" + System.currentTimeMillis())
                .category(existing.getCategory())
                .price(java.math.BigDecimal.valueOf(199.99))
                .reorderLevel(5)
                .active(true)
                .build());

        // Setup source inventory with 30 units, 0 reserved
        Location sourceLoc = locationRepository.findById(sourceLocationId).orElseThrow();
        Inventory srcInv = inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .location(sourceLoc)
                .quantity(30)
                .reservedQuantity(0)
                .build());

        // Ensure NO inventory exists at destination location
        assertTrue(inventoryRepository.findByProductIdAndLocationId(testProduct.getId(), destinationLocationId).isEmpty());

        String transferNum = "TRF-DEST-INIT-" + System.currentTimeMillis();
        int transferQty = 12;

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProduct.getId(),
                sourceLocationId,
                destinationLocationId,
                transferQty,
                "COMPLETED",
                "Destination auto-initialization test"
        );

        StockTransferResponse response = stockTransferService.createTransfer(request);
        assertNotNull(response);
        assertEquals("COMPLETED", response.status());

        // Verify destination inventory record was created and has correct stock
        Inventory createdDestInv = inventoryRepository.findByProductIdAndLocationId(testProduct.getId(), destinationLocationId).orElseThrow();
        assertEquals(transferQty, createdDestInv.getQuantity());
        assertEquals(0, createdDestInv.getReservedQuantity());

        // Verify source inventory was decremented
        Inventory updatedSrc = inventoryRepository.findById(srcInv.getId()).orElseThrow();
        assertEquals(30 - transferQty, updatedSrc.getQuantity());

        // Verify dual StockMovement audit records
        List<StockMovement> movements = stockMovementRepository.findAll();
        StockMovement lastDest = movements.get(movements.size() - 1);
        StockMovement lastSrc = movements.get(movements.size() - 2);

        assertEquals("TRANSFER", lastDest.getMovementType());
        assertEquals(destinationLocationId, lastDest.getLocation().getId());
        assertEquals(transferQty, lastDest.getQuantity());

        assertEquals("TRANSFER", lastSrc.getMovementType());
        assertEquals(sourceLocationId, lastSrc.getLocation().getId());
        assertEquals(transferQty, lastSrc.getQuantity());
    }

    @Test
    @DisplayName("Verify two concurrent transfers competing for insufficient source stock: one succeeds, one fails, no overselling")
    void testConcurrentTransfersCompetingForInsufficientStock() throws Exception {
        Product existing = productRepository.findById(testProductId).orElseThrow();
        Product testProduct = productRepository.save(Product.builder()
                .name("Concurrency Competing Product " + System.currentTimeMillis())
                .sku("CONC-COMP-" + System.currentTimeMillis())
                .category(existing.getCategory())
                .price(java.math.BigDecimal.valueOf(250.00))
                .reorderLevel(5)
                .active(true)
                .build());

        Location sourceLoc = locationRepository.findById(sourceLocationId).orElseThrow();
        Location destLoc = locationRepository.findById(destinationLocationId).orElseThrow();

        // Source has exactly 15 units, 0 reserved
        Inventory srcInv = inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .location(sourceLoc)
                .quantity(15)
                .reservedQuantity(0)
                .build());

        // Destination has 0 units initially
        Inventory destInv = inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .location(destLoc)
                .quantity(0)
                .reservedQuantity(0)
                .build());

        int requestedQty = 10; // Both request 10 (total 20 > 15 available)
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 1; i <= 2; i++) {
            final String trfNum = "TRF-COMPETE-" + i + "-" + System.currentTimeMillis();
            executor.submit(() -> {
                try {
                    startLatch.await();
                    StockTransferRequest req = new StockTransferRequest(
                            trfNum,
                            LocalDateTime.now(),
                            testProduct.getId(),
                            sourceLocationId,
                            destinationLocationId,
                            requestedQty,
                            "COMPLETED",
                            "Concurrent competing transfer"
                    );
                    stockTransferService.createTransfer(req);
                    successCount.incrementAndGet();
                } catch (InvalidStockOperationException ex) {
                    failCount.incrementAndGet();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire both threads simultaneously
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Both transfer threads should complete within timeout");
        assertEquals(1, successCount.get(), "Exactly one transfer should succeed");
        assertEquals(1, failCount.get(), "Exactly one transfer should fail due to insufficient stock");

        // Verify source inventory deducted exactly by 10 (15 - 10 = 5, not -5)
        Inventory finalSrc = inventoryRepository.findById(srcInv.getId()).orElseThrow();
        assertEquals(5, finalSrc.getQuantity());

        // Verify destination incremented exactly by 10 (0 + 10 = 10)
        Inventory finalDest = inventoryRepository.findById(destInv.getId()).orElseThrow();
        assertEquals(10, finalDest.getQuantity());

        // Total inventory across facilities invariant: 5 + 10 = 15
        assertEquals(15, finalSrc.getQuantity() + finalDest.getQuantity());
    }

    @Test
    @DisplayName("Verify concurrent transfers in opposite directions execute without deadlock due to lock ordering")
    void testConcurrentTransfersOppositeDirectionsDeadlockFree() throws Exception {
        Product existing = productRepository.findById(testProductId).orElseThrow();
        Product testProduct = productRepository.save(Product.builder()
                .name("Opposite Directions Product " + System.currentTimeMillis())
                .sku("OPP-DIR-" + System.currentTimeMillis())
                .category(existing.getCategory())
                .price(java.math.BigDecimal.valueOf(300.00))
                .reorderLevel(5)
                .active(true)
                .build());

        Location loc1 = locationRepository.findById(sourceLocationId).orElseThrow();
        Location loc2 = locationRepository.findById(destinationLocationId).orElseThrow();

        // Loc1 has 50 units, Loc2 has 50 units
        Inventory inv1 = inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .location(loc1)
                .quantity(50)
                .reservedQuantity(0)
                .build());

        Inventory inv2 = inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .location(loc2)
                .quantity(50)
                .reservedQuantity(0)
                .build());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Loc1 -> Loc2 (10 units)
        executor.submit(() -> {
            try {
                startLatch.await();
                StockTransferRequest req = new StockTransferRequest(
                        "TRF-OPP-1-" + System.currentTimeMillis(),
                        LocalDateTime.now(),
                        testProduct.getId(),
                        loc1.getId(),
                        loc2.getId(),
                        10,
                        "COMPLETED",
                        "Opposite direction 1 -> 2"
                );
                stockTransferService.createTransfer(req);
                successCount.incrementAndGet();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Loc2 -> Loc1 (10 units)
        executor.submit(() -> {
            try {
                startLatch.await();
                StockTransferRequest req = new StockTransferRequest(
                        "TRF-OPP-2-" + System.currentTimeMillis(),
                        LocalDateTime.now(),
                        testProduct.getId(),
                        loc2.getId(),
                        loc1.getId(),
                        10,
                        "COMPLETED",
                        "Opposite direction 2 -> 1"
                );
                stockTransferService.createTransfer(req);
                successCount.incrementAndGet();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Opposite direction transfers must complete without deadlock");
        assertEquals(2, successCount.get(), "Both opposite-direction transfers should succeed");

        Inventory finalInv1 = inventoryRepository.findById(inv1.getId()).orElseThrow();
        Inventory finalInv2 = inventoryRepository.findById(inv2.getId()).orElseThrow();

        // Net change: Loc1 = 50 - 10 + 10 = 50, Loc2 = 50 + 10 - 10 = 50
        assertEquals(50, finalInv1.getQuantity());
        assertEquals(50, finalInv2.getQuantity());
        assertEquals(100, finalInv1.getQuantity() + finalInv2.getQuantity());
    }

    @Test
    @DisplayName("Verify transfer competing with concurrent stock adjustment: row lock prevents lost updates/negative stock")
    void testTransferCompetingWithStockAdjustment() throws Exception {
        Product existing = productRepository.findById(testProductId).orElseThrow();
        Product testProduct = productRepository.save(Product.builder()
                .name("Transfer Vs Adjustment " + System.currentTimeMillis())
                .sku("TRF-ADJ-" + System.currentTimeMillis())
                .category(existing.getCategory())
                .price(java.math.BigDecimal.valueOf(150.00))
                .reorderLevel(5)
                .active(true)
                .build());

        Location sourceLoc = locationRepository.findById(sourceLocationId).orElseThrow();
        Location destLoc = locationRepository.findById(destinationLocationId).orElseThrow();

        // Source has 20 units
        Inventory srcInv = inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .location(sourceLoc)
                .quantity(20)
                .reservedQuantity(0)
                .build());

        Inventory destInv = inventoryRepository.save(Inventory.builder()
                .product(testProduct)
                .location(destLoc)
                .quantity(0)
                .reservedQuantity(0)
                .build());

        // Transfer wants 15, Adjustment wants -10 (Total attempted 25 > 20)
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Transfer 15 units
        executor.submit(() -> {
            try {
                startLatch.await();
                StockTransferRequest req = new StockTransferRequest(
                        "TRF-VS-ADJ-" + System.currentTimeMillis(),
                        LocalDateTime.now(),
                        testProduct.getId(),
                        sourceLocationId,
                        destinationLocationId,
                        15,
                        "COMPLETED",
                        "Transfer competing with adjustment"
                );
                stockTransferService.createTransfer(req);
                successCount.incrementAndGet();
            } catch (InvalidStockOperationException ex) {
                failCount.incrementAndGet();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Adjustment -10 units
        executor.submit(() -> {
            try {
                startLatch.await();
                StockAdjustmentRequest req = new StockAdjustmentRequest(
                        testProduct.getId(),
                        sourceLocationId,
                        -10,
                        "ADJ-VS-TRF",
                        "Adjustment competing with transfer"
                );
                inventoryService.adjustStock(req);
                successCount.incrementAndGet();
            } catch (InvalidStockOperationException ex) {
                failCount.incrementAndGet();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Both operations must complete within timeout");
        assertEquals(1, successCount.get(), "Exactly one operation should succeed");
        assertEquals(1, failCount.get(), "Exactly one operation should be rejected due to insufficient stock");

        Inventory finalSrc = inventoryRepository.findById(srcInv.getId()).orElseThrow();
        Inventory finalDest = inventoryRepository.findById(destInv.getId()).orElseThrow();

        // Resulting quantity must never be negative!
        assertTrue(finalSrc.getQuantity() >= 0, "Source inventory quantity must never be negative");
        assertTrue(finalSrc.getQuantity() == 5 || finalSrc.getQuantity() == 10,
                "Final source balance must be either 5 (transfer won) or 10 (adjustment won)");
    }

    @Test
    @DisplayName("Verify rejection of unsupported or ambiguous transfer status (e.g. PENDING, DRAFT)")
    void testInvalidTransferStatusRejection() {
        String transferNum = "TRF-INVALID-STATUS-" + System.currentTimeMillis();
        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                destinationLocationId,
                5,
                "PENDING", // Unsupported status
                "Invalid status test"
        );

        long initialTransfers = stockTransferRepository.count();
        long initialMovements = stockMovementRepository.count();

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> stockTransferService.createTransfer(request)
        );

        assertTrue(ex.getMessage().contains("Invalid transfer status: 'PENDING'. Supported statuses are COMPLETED, CANCELLED."));
        assertEquals(initialTransfers, stockTransferRepository.count());
        assertEquals(initialMovements, stockMovementRepository.count());
    }

    @Test
    @DisplayName("Verify CANCELLED transfer creation persists record without modifying inventory or creating movements")
    void testCancelledTransferCreationDoesNotModifyInventory() {
        Inventory srcBefore = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        Inventory destBefore = inventoryRepository.findById(destinationInventoryId).orElseThrow();
        int initialSrcQty = srcBefore.getQuantity();
        int initialDestQty = destBefore.getQuantity();
        long initialMovements = stockMovementRepository.count();
        long initialTransfers = stockTransferRepository.count();

        String transferNum = "TRF-CANCELLED-CREATE-" + System.currentTimeMillis();
        int transferQty = 10;

        StockTransferRequest request = new StockTransferRequest(
                transferNum,
                LocalDateTime.now(),
                testProductId,
                sourceLocationId,
                destinationLocationId,
                transferQty,
                "CANCELLED",
                "Created as cancelled test"
        );

        StockTransferResponse response = stockTransferService.createTransfer(request);

        assertNotNull(response);
        assertEquals("CANCELLED", response.status());
        assertEquals(transferNum, response.transferNumber());
        assertEquals(initialTransfers + 1, stockTransferRepository.count());

        // Verify inventory records are STRICTLY unchanged
        Inventory srcAfter = inventoryRepository.findById(sourceInventoryId).orElseThrow();
        Inventory destAfter = inventoryRepository.findById(destinationInventoryId).orElseThrow();
        assertEquals(initialSrcQty, srcAfter.getQuantity(), "Source quantity must remain unchanged for CANCELLED transfer");
        assertEquals(initialDestQty, destAfter.getQuantity(), "Destination quantity must remain unchanged for CANCELLED transfer");

        // Verify NO stock movements created
        assertEquals(initialMovements, stockMovementRepository.count(), "Zero StockMovement records must be created for CANCELLED transfer");
    }
}

