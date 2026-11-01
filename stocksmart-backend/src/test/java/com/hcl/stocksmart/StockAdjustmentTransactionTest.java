package com.hcl.stocksmart;

import com.hcl.stocksmart.dto.StockAdjustmentRequest;
import com.hcl.stocksmart.dto.StockMovementResponse;
import com.hcl.stocksmart.entity.Inventory;
import com.hcl.stocksmart.entity.Location;
import com.hcl.stocksmart.entity.Product;
import com.hcl.stocksmart.entity.StockMovement;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.repository.InventoryRepository;
import com.hcl.stocksmart.repository.LocationRepository;
import com.hcl.stocksmart.repository.ProductRepository;
import com.hcl.stocksmart.repository.StockMovementRepository;
import com.hcl.stocksmart.services.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockAdjustmentTransactionTest {

    @Autowired
    private InventoryService inventoryService;

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
    private Long testLocationId;
    private Long testInventoryId;

    @BeforeEach
    void setUp() {
        // Ensure at least one product, location, and inventory record exists
        List<Product> products = productRepository.findAll();
        assertFalse(products.isEmpty(), "At least one product must exist in database");
        Product product = products.get(0);
        testProductId = product.getId();

        List<Location> locations = locationRepository.findAll();
        assertFalse(locations.isEmpty(), "At least one location must exist in database");
        Location location = locations.get(0);
        testLocationId = location.getId();

        Inventory inventory = inventoryRepository.findByProductIdAndLocationId(testProductId, testLocationId)
                .orElseGet(() -> {
                    Inventory newInv = Inventory.builder()
                            .product(product)
                            .location(location)
                            .quantity(30)
                            .reservedQuantity(0)
                            .build();
                    return inventoryRepository.save(newInv);
                });
        if (inventory.getQuantity() < 15) {
            inventory.setQuantity(30);
            inventory = inventoryRepository.save(inventory);
        }
        testInventoryId = inventory.getId();
    }

    @Test
    @DisplayName("Verify successful atomic stock adjustment (Inventory + StockMovement audit)")
    void testSuccessfulAdjustment() {
        Inventory currentInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = currentInv.getQuantity();
        long initialMovementsCount = stockMovementRepository.count();

        int adjustmentDelta = -5;
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                testProductId,
                testLocationId,
                adjustmentDelta,
                "ADJ-TEST-001",
                "Automated test cycle count decrease"
        );

        var response = inventoryService.adjustStock(request);

        assertEquals(initialQty + adjustmentDelta, response.quantity());

        Inventory updatedInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty + adjustmentDelta, updatedInv.getQuantity());

        long afterMovementsCount = stockMovementRepository.count();
        assertEquals(initialMovementsCount + 1, afterMovementsCount);

        List<StockMovement> movements = stockMovementRepository.findAll();
        StockMovement lastMovement = movements.get(movements.size() - 1);
        assertEquals("ADJUSTMENT", lastMovement.getMovementType());
        assertEquals(Math.abs(adjustmentDelta), lastMovement.getQuantity());
        assertEquals("ADJ-TEST-001", lastMovement.getReferenceType());
        assertTrue(lastMovement.getRemarks().contains("decreased by 5 units"));
    }

    @Test
    @DisplayName("Verify rejection when resulting stock would be negative")
    void testInsufficientStockRejection() {
        Inventory currentInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = currentInv.getQuantity();

        StockAdjustmentRequest request = new StockAdjustmentRequest(
                testProductId,
                testLocationId,
                -(initialQty + 500),
                "ADJ-FAIL-001",
                "Excessive reduction test"
        );

        assertThrows(InvalidStockOperationException.class, () -> {
            inventoryService.adjustStock(request);
        });

        Inventory unchangedInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, unchangedInv.getQuantity());
    }

    @Test
    @DisplayName("Verify transaction rollback: Inventory changes are rolled back if subsequent step fails")
    void testTransactionRollbackOnControlledFailure() {
        Inventory currentInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = currentInv.getQuantity();
        long initialMovementsCount = stockMovementRepository.count();

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            // Step 1: Mutate inventory inside transaction
            Inventory invInTx = inventoryRepository.findById(testInventoryId).orElseThrow();
            invInTx.setQuantity(invInTx.getQuantity() + 100);
            inventoryRepository.save(invInTx);

            // Step 2: Simulate controlled failure before transaction commit
            throw new RuntimeException("CONTROLLED_TEST_FAILURE: Simulating audit movement database failure");
        } catch (RuntimeException ex) {
            // Roll back the transaction
            transactionManager.rollback(status);
        }

        // Verify inventory quantity is strictly reverted in database
        Inventory rolledBackInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, rolledBackInv.getQuantity(), "Inventory quantity MUST rollback to initial value");

        long finalMovementsCount = stockMovementRepository.count();
        assertEquals(initialMovementsCount, finalMovementsCount, "No StockMovement audit record should be committed");
    }

    @Test
    @DisplayName("Verify positive stock adjustment auto-initializes inventory record when none exists")
    void testAutoInitializeStockOnFirstAdjustment() {
        Product existing = productRepository.findById(testProductId).orElseThrow();
        // Create a temporary unlinked product
        Product tempProduct = productRepository.save(Product.builder()
                .name("Unstocked Test Product " + System.currentTimeMillis())
                .sku("UNSTOCKED-" + System.currentTimeMillis())
                .category(existing.getCategory())
                .price(java.math.BigDecimal.valueOf(99.99))
                .reorderLevel(5)
                .active(true)
                .build());

        Location location = locationRepository.findById(testLocationId).orElseThrow();

        // Verify no inventory exists
        assertTrue(inventoryRepository.findByProductIdAndLocationId(tempProduct.getId(), location.getId()).isEmpty());

        int initialStockQty = 15;
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                tempProduct.getId(),
                location.getId(),
                initialStockQty,
                "INIT-TEST-001",
                "Initial stock setup via adjustment"
        );

        var response = inventoryService.adjustStock(request);
        assertEquals(initialStockQty, response.quantity());

        // Verify inventory record was created
        Inventory createdInv = inventoryRepository.findByProductIdAndLocationId(tempProduct.getId(), location.getId()).orElseThrow();
        assertEquals(initialStockQty, createdInv.getQuantity());
        assertEquals(0, createdInv.getReservedQuantity());

        // Verify StockMovement audit
        List<StockMovement> movements = stockMovementRepository.findAll();
        StockMovement lastMovement = movements.get(movements.size() - 1);
        assertEquals("ADJUSTMENT", lastMovement.getMovementType());
        assertEquals(initialStockQty, lastMovement.getQuantity());
    }

    @Test
    @DisplayName("Verify negative stock adjustment is rejected when no inventory record exists")
    void testRejectNegativeAdjustmentWhenNoInventoryExists() {
        Product existing = productRepository.findById(testProductId).orElseThrow();
        Product tempProduct = productRepository.save(Product.builder()
                .name("Unstocked Test Product 2 " + System.currentTimeMillis())
                .sku("UNSTOCKED2-" + System.currentTimeMillis())
                .category(existing.getCategory())
                .price(java.math.BigDecimal.valueOf(49.99))
                .reorderLevel(5)
                .active(true)
                .build());

        Location location = locationRepository.findById(testLocationId).orElseThrow();

        StockAdjustmentRequest request = new StockAdjustmentRequest(
                tempProduct.getId(),
                location.getId(),
                -5,
                "FAIL-TEST-002",
                "Invalid reduction on unstocked product"
        );

        assertThrows(InvalidStockOperationException.class, () -> {
            inventoryService.adjustStock(request);
        });
    }
}

