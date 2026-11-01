package com.hcl.stocksmart;

import com.hcl.stocksmart.dto.PurchaseReceivingItemRequest;
import com.hcl.stocksmart.dto.PurchaseReceivingRequest;
import com.hcl.stocksmart.dto.PurchaseReceivingResponse;
import com.hcl.stocksmart.entity.*;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.*;
import com.hcl.stocksmart.services.PurchaseReceivingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PurchaseReceivingTransactionTest {

    @Autowired
    private PurchaseReceivingService purchaseReceivingService;

    @Autowired
    private PurchaseReceivingRepository purchaseReceivingRepository;

    @Autowired
    private PurchaseReceivingItemRepository purchaseReceivingItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long testSupplierId;
    private Long testLocationId;
    private Long testProductId;
    private Long testInventoryId;

    @BeforeEach
    void setUp() {
        Supplier supplier = supplierRepository.findAll().stream().findFirst().orElseGet(() -> {
            Supplier s = Supplier.builder()
                    .name("Test Supplier Ltd")
                    .email("john@testsupplier.com")
                    .phone("+91 9876543210")
                    .address("Bangalore, India")
                    .build();
            return supplierRepository.save(s);
        });
        testSupplierId = supplier.getId();

        Location location = locationRepository.findAll().stream().findFirst().orElseThrow();
        testLocationId = location.getId();

        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        testProductId = product.getId();

        Inventory inventory = inventoryRepository.findByProductIdAndLocationId(testProductId, testLocationId)
                .orElseGet(() -> {
                    Inventory inv = Inventory.builder()
                            .product(product)
                            .location(location)
                            .quantity(10)
                            .reservedQuantity(0)
                            .build();
                    return inventoryRepository.save(inv);
                });
        testInventoryId = inventory.getId();
    }

    @Test
    @DisplayName("Verify completed purchase receiving atomically updates receivings, items, inventory, and stock movements")
    void testSuccessfulReceivingWorkflow() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        int initialReserved = initialInv.getReservedQuantity();
        long initialMovements = stockMovementRepository.count();

        String receivingNumber = "RCV-TEST-" + System.currentTimeMillis();
        int receivedQty = 15;
        BigDecimal unitCost = new BigDecimal("4500.00");

        PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(testProductId, receivedQty, unitCost);
        PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                receivingNumber,
                LocalDateTime.now(),
                testSupplierId,
                testLocationId,
                "RECEIVED",
                new BigDecimal("67500.00"),
                "Phase 8 Integration Test Receipt",
                List.of(itemReq)
        );

        PurchaseReceivingResponse response = purchaseReceivingService.createReceiving(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(receivingNumber, response.receivingNumber());
        assertEquals(1, response.items().size());
        assertEquals(0, new BigDecimal("67500.00").compareTo(response.totalAmount()));

        // Verify Inventory increment and reservedQuantity unchanged
        Inventory updatedInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty + receivedQty, updatedInv.getQuantity());
        assertEquals(initialReserved, updatedInv.getReservedQuantity());

        // Verify StockMovement audit record
        assertEquals(initialMovements + 1, stockMovementRepository.count());
        List<StockMovement> movements = stockMovementRepository.findAll();
        StockMovement lastMovement = movements.get(movements.size() - 1);
        assertEquals("RECEIPT", lastMovement.getMovementType());
        assertEquals(receivedQty, lastMovement.getQuantity());
        assertEquals(receivingNumber, lastMovement.getReferenceType());
        assertEquals(response.id(), lastMovement.getReferenceId());
        assertEquals(testProductId, lastMovement.getProduct().getId());
        assertEquals(testLocationId, lastMovement.getLocation().getId());
    }

    @Test
    @DisplayName("Verify duplicate receiving number is rejected with DuplicateResourceException (HTTP 409)")
    void testDuplicateReceivingNumberRejection() {
        String receivingNumber = "RCV-DUP-" + System.currentTimeMillis();
        int receivedQty = 5;
        BigDecimal unitCost = new BigDecimal("1200.00");

        PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(testProductId, receivedQty, unitCost);
        PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                receivingNumber,
                LocalDateTime.now(),
                testSupplierId,
                testLocationId,
                "RECEIVED",
                new BigDecimal("6000.00"),
                "Initial receipt",
                List.of(itemReq)
        );

        // First attempt succeeds
        PurchaseReceivingResponse response = purchaseReceivingService.createReceiving(request);
        assertNotNull(response);

        Inventory invAfterFirst = inventoryRepository.findById(testInventoryId).orElseThrow();
        int qtyAfterFirst = invAfterFirst.getQuantity();
        long movementsAfterFirst = stockMovementRepository.count();

        // Second attempt with duplicate receivingNumber must throw DuplicateResourceException
        assertThrows(DuplicateResourceException.class, () -> {
            purchaseReceivingService.createReceiving(request);
        });

        // Verify inventory and movements did not change
        Inventory invAfterSecond = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(qtyAfterFirst, invAfterSecond.getQuantity());
        assertEquals(movementsAfterFirst, stockMovementRepository.count());
    }

    @Test
    @DisplayName("Verify receiving with invalid product ID fails and makes no changes")
    void testInvalidProductRejection() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        long initialReceivings = purchaseReceivingRepository.count();

        String receivingNumber = "RCV-INV-PROD-" + System.currentTimeMillis();
        Long nonExistentProductId = 999999L;

        PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(nonExistentProductId, 5, new BigDecimal("100.00"));
        PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                receivingNumber,
                LocalDateTime.now(),
                testSupplierId,
                testLocationId,
                "RECEIVED",
                new BigDecimal("500.00"),
                "Invalid product test",
                List.of(itemReq)
        );

        assertThrows(ResourceNotFoundException.class, () -> {
            purchaseReceivingService.createReceiving(request);
        });

        assertEquals(initialReceivings, purchaseReceivingRepository.count());
        Inventory invAfter = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, invAfter.getQuantity());
    }

    @Test
    @DisplayName("Verify receiving with invalid quantity fails validation")
    void testInvalidQuantityRejection() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        long initialReceivings = purchaseReceivingRepository.count();

        String receivingNumber = "RCV-INV-QTY-" + System.currentTimeMillis();

        PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(testProductId, 0, new BigDecimal("100.00"));
        PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                receivingNumber,
                LocalDateTime.now(),
                testSupplierId,
                testLocationId,
                "RECEIVED",
                BigDecimal.ZERO,
                "Invalid quantity test",
                List.of(itemReq)
        );

        assertThrows(InvalidStockOperationException.class, () -> {
            purchaseReceivingService.createReceiving(request);
        });

        assertEquals(initialReceivings, purchaseReceivingRepository.count());
        Inventory invAfter = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, invAfter.getQuantity());
    }

    @Test
    @DisplayName("Verify atomic transaction rollback when an error occurs during receipt processing")
    void testControlledTransactionRollback() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        long initialReceivings = purchaseReceivingRepository.count();
        long initialItems = purchaseReceivingItemRepository.count();
        long initialMovements = stockMovementRepository.count();

        String receivingNumber = "RCV-ROLLBACK-" + System.currentTimeMillis();

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(testProductId, 10, new BigDecimal("1000.00"));
            PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                    receivingNumber,
                    LocalDateTime.now(),
                    testSupplierId,
                    testLocationId,
                    "RECEIVED",
                    new BigDecimal("10000.00"),
                    "Rollback test",
                    List.of(itemReq)
            );

            purchaseReceivingService.createReceiving(request);
            // Simulate abnormal failure after partial execution
            throw new RuntimeException("Simulated unexpected failure during receiving workflow");
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
        }

        // Verify everything was rolled back
        assertEquals(initialReceivings, purchaseReceivingRepository.count());
        assertEquals(initialItems, purchaseReceivingItemRepository.count());
        assertEquals(initialMovements, stockMovementRepository.count());
        Inventory invAfter = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, invAfter.getQuantity());
    }

    @Test
    @DisplayName("Phase 8.1: Verify RECEIVED purchase receiving cannot be deleted and returns HTTP 409")
    void testReceivedReceivingCannotBeDeleted() {
        // 1. Create a RECEIVED receipt
        String receivingNumber = "RCV-DEL-TEST-" + System.currentTimeMillis();
        int receivedQty = 8;
        BigDecimal unitCost = new BigDecimal("2000.00");

        PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(testProductId, receivedQty, unitCost);
        PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                receivingNumber,
                LocalDateTime.now(),
                testSupplierId,
                testLocationId,
                "RECEIVED",
                new BigDecimal("16000.00"),
                "Receipt deletion test",
                List.of(itemReq)
        );

        PurchaseReceivingResponse created = purchaseReceivingService.createReceiving(request);
        assertNotNull(created.id());

        Inventory invAfterReceipt = inventoryRepository.findById(testInventoryId).orElseThrow();
        int qtyAfterReceipt = invAfterReceipt.getQuantity();
        long itemsAfterReceipt = purchaseReceivingItemRepository.count();
        long movementsAfterReceipt = stockMovementRepository.count();

        // 2. Attempt deletion - must throw InvalidStockOperationException (HTTP 409)
        InvalidStockOperationException exception = assertThrows(
                InvalidStockOperationException.class,
                () -> purchaseReceivingService.deleteReceiving(created.id())
        );

        assertTrue(exception.getMessage().contains("Received purchase receipts cannot be deleted because they affect inventory history."));

        // 3. Verify receiving header remains in DB
        assertTrue(purchaseReceivingRepository.findById(created.id()).isPresent());

        // 4. Verify receiving items remain in DB
        assertEquals(itemsAfterReceipt, purchaseReceivingItemRepository.count());

        // 5. Verify inventory quantity remains intact (unchanged)
        Inventory invAfterDeleteAttempt = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(qtyAfterReceipt, invAfterDeleteAttempt.getQuantity());

        // 6. Verify stock movement audit log remains intact
        assertEquals(movementsAfterReceipt, stockMovementRepository.count());
    }

    @Test
    @DisplayName("Phase 8.1: Verify CANCELLED receiving does not increase inventory or create movements")
    void testCancelledReceivingDoesNotIncreaseInventory() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        long initialMovements = stockMovementRepository.count();

        String receivingNumber = "RCV-CANCELLED-" + System.currentTimeMillis();
        int qty = 12;
        BigDecimal unitCost = new BigDecimal("1500.00");

        PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(testProductId, qty, unitCost);
        PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                receivingNumber,
                LocalDateTime.now(),
                testSupplierId,
                testLocationId,
                "CANCELLED",
                new BigDecimal("18000.00"),
                "Pre-cancelled intake test",
                List.of(itemReq)
        );

        PurchaseReceivingResponse created = purchaseReceivingService.createReceiving(request);
        assertNotNull(created.id());
        assertEquals("CANCELLED", created.status());

        // Verify receiving header and items were saved
        assertTrue(purchaseReceivingRepository.findById(created.id()).isPresent());
        assertEquals(1, purchaseReceivingItemRepository.findByReceivingId(created.id()).size());

        // Verify inventory quantity was NOT increased
        Inventory currentInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, currentInv.getQuantity());

        // Verify no stock movement audit record was created
        assertEquals(initialMovements, stockMovementRepository.count());
    }

    @Test
    @DisplayName("Phase 8.1: Verify RECEIVED receiving cannot be changed into CANCELLED")
    void testReceivedReceivingCannotBeCancelled() {
        String receivingNumber = "RCV-CAN-CHANGE-" + System.currentTimeMillis();
        int receivedQty = 6;
        BigDecimal unitCost = new BigDecimal("3000.00");

        PurchaseReceivingItemRequest itemReq = new PurchaseReceivingItemRequest(testProductId, receivedQty, unitCost);
        PurchaseReceivingRequest request = new PurchaseReceivingRequest(
                receivingNumber,
                LocalDateTime.now(),
                testSupplierId,
                testLocationId,
                "RECEIVED",
                new BigDecimal("18000.00"),
                "Receipt cannot cancel test",
                List.of(itemReq)
        );

        PurchaseReceivingResponse created = purchaseReceivingService.createReceiving(request);
        assertNotNull(created.id());
        assertEquals("RECEIVED", created.status());

        // Attempting to cancel an already RECEIVED receipt must be rejected with 409
        InvalidStockOperationException exception = assertThrows(
                InvalidStockOperationException.class,
                () -> purchaseReceivingService.cancelReceiving(created.id())
        );

        assertTrue(exception.getMessage().contains("Received purchase receipts cannot be cancelled because they affect inventory history."));

        // Status must remain RECEIVED
        PurchaseReceiving receivingInDb = purchaseReceivingRepository.findById(created.id()).orElseThrow();
        assertEquals("RECEIVED", receivingInDb.getStatus());
    }
}
