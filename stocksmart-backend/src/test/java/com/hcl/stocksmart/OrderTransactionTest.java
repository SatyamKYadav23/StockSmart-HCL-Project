package com.hcl.stocksmart;

import com.hcl.stocksmart.dto.OrderItemRequest;
import com.hcl.stocksmart.dto.OrderRequest;
import com.hcl.stocksmart.dto.OrderResponse;
import com.hcl.stocksmart.entity.Inventory;
import com.hcl.stocksmart.entity.Location;
import com.hcl.stocksmart.entity.Order;
import com.hcl.stocksmart.entity.OrderItem;
import com.hcl.stocksmart.entity.Product;
import com.hcl.stocksmart.entity.StockMovement;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.repository.*;
import com.hcl.stocksmart.services.OrderService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderTransactionTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

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
        Product product = productRepository.findAll().get(0);
        testProductId = product.getId();

        Location location = locationRepository.findAll().get(0);
        testLocationId = location.getId();

        Inventory inventory = inventoryRepository.findByProductIdAndLocationId(testProductId, testLocationId)
                .orElseGet(() -> {
                    Inventory newInv = Inventory.builder()
                            .product(product)
                            .location(location)
                            .quantity(20)
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
    @DisplayName("Verify completed order atomically updates orders, order_items, inventory, and stock_movements")
    void testSuccessfulCompletedOrderWorkflow() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        long initialMovements = stockMovementRepository.count();

        String orderNum = "ORD-TEST-" + System.currentTimeMillis();
        int orderItemQty = 4;
        BigDecimal unitPrice = new BigDecimal("12999.00");

        OrderItemRequest itemReq = new OrderItemRequest(testProductId, orderItemQty, unitPrice);
        OrderRequest request = new OrderRequest(
                orderNum,
                "COMPLETED",
                "Acme Retail Counter",
                testLocationId,
                null,
                "Manual sales order entry",
                List.of(itemReq)
        );

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response.id());
        assertEquals(orderNum, response.orderNumber());
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("51996.00"), response.totalAmount());

        // 1. Verify inventory deducted
        Inventory updatedInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty - orderItemQty, updatedInv.getQuantity());

        // 2. Verify stock movement created
        assertEquals(initialMovements + 1, stockMovementRepository.count());
        List<StockMovement> movements = stockMovementRepository.findAll();
        StockMovement lastMovement = movements.get(movements.size() - 1);
        assertEquals("SALE", lastMovement.getMovementType());
        assertEquals(orderItemQty, lastMovement.getQuantity());
        assertEquals(orderNum, lastMovement.getReferenceType());
    }

    @Test
    @DisplayName("Verify insufficient stock rejects order and mutates zero state")
    void testInsufficientStockRejection() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        long initialOrders = orderRepository.count();

        String orderNum = "ORD-EXCESS-" + System.currentTimeMillis();
        OrderItemRequest excessiveItem = new OrderItemRequest(testProductId, initialQty + 1000, new BigDecimal("100.00"));
        OrderRequest request = new OrderRequest(
                orderNum,
                "COMPLETED",
                "Large Purchaser",
                testLocationId,
                null,
                "Attempting more than on-hand",
                List.of(excessiveItem)
        );

        assertThrows(InvalidStockOperationException.class, () -> {
            orderService.createOrder(request);
        });

        // Verify inventory untouched
        Inventory currentInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, currentInv.getQuantity());

        // Verify no order created
        assertEquals(initialOrders, orderRepository.count());
        assertFalse(orderRepository.existsByOrderNumber(orderNum));
    }

    @Test
    @DisplayName("Verify transaction rollback: Order, items, and inventory rollback on controlled failure")
    void testOrderTransactionRollbackOnControlledFailure() {
        Inventory initialInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        int initialQty = initialInv.getQuantity();
        long initialOrders = orderRepository.count();
        long initialItems = orderItemRepository.count();
        long initialMovements = stockMovementRepository.count();

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        String testOrderNum = "ORD-ROLLBACK-" + System.currentTimeMillis();

        try {
            // Step 1: Create Order
            Order order = Order.builder()
                    .orderNumber(testOrderNum)
                    .status("COMPLETED")
                    .customerName("Rollback Test Customer")
                    .totalAmount(new BigDecimal("999.00"))
                    .build();
            order = orderRepository.save(order);

            // Step 2: Create OrderItem
            Product product = productRepository.findById(testProductId).orElseThrow();
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(3)
                    .unitPrice(new BigDecimal("333.00"))
                    .subtotal(new BigDecimal("999.00"))
                    .build();
            orderItemRepository.save(item);

            // Step 3: Mutate inventory
            Inventory inv = inventoryRepository.findById(testInventoryId).orElseThrow();
            inv.setQuantity(inv.getQuantity() - 3);
            inventoryRepository.save(inv);

            // Step 4: Simulate unexpected exception before commit
            throw new RuntimeException("CONTROLLED_TEST_FAILURE: Simulating failure before order transaction commits");
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
        }

        // Verify all entities rolled back
        assertFalse(orderRepository.existsByOrderNumber(testOrderNum));
        assertEquals(initialOrders, orderRepository.count());
        assertEquals(initialItems, orderItemRepository.count());
        assertEquals(initialMovements, stockMovementRepository.count());

        Inventory rolledBackInv = inventoryRepository.findById(testInventoryId).orElseThrow();
        assertEquals(initialQty, rolledBackInv.getQuantity(), "Inventory must rollback to original quantity");
    }

    @Test
    @DisplayName("Completed orders cannot be deleted to preserve inventory history")
    void testCompletedOrderCannotBeDeleted() {
        String orderNum = "ORD-IMMUT-DEL-" + System.currentTimeMillis();
        OrderItemRequest itemReq = new OrderItemRequest(testProductId, 2, BigDecimal.valueOf(100.00));
        OrderRequest request = new OrderRequest(
                orderNum,
                "COMPLETED",
                "Immutability Customer",
                testLocationId,
                BigDecimal.valueOf(200.00),
                "Immutability Delete Test",
                List.of(itemReq)
        );

        OrderResponse created = orderService.createOrder(request);
        assertNotNull(created.id());

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> orderService.deleteOrder(created.id())
        );
        assertTrue(ex.getMessage().contains("Completed orders cannot be deleted"));
    }

    @Test
    @DisplayName("Completed orders cannot change status to avoid corrupting inventory history")
    void testCompletedOrderCannotChangeStatus() {
        String orderNum = "ORD-IMMUT-STATUS-" + System.currentTimeMillis();
        OrderItemRequest itemReq = new OrderItemRequest(testProductId, 1, BigDecimal.valueOf(50.00));
        OrderRequest request = new OrderRequest(
                orderNum,
                "COMPLETED",
                "Status Test Customer",
                testLocationId,
                BigDecimal.valueOf(50.00),
                "Immutability Status Test",
                List.of(itemReq)
        );

        OrderResponse created = orderService.createOrder(request);
        assertNotNull(created.id());

        OrderRequest updateReq = new OrderRequest(
                orderNum,
                "PENDING",
                "Status Test Customer",
                testLocationId,
                BigDecimal.valueOf(50.00),
                "Attempted Status Change",
                List.of(itemReq)
        );

        InvalidStockOperationException ex = assertThrows(
                InvalidStockOperationException.class,
                () -> orderService.updateOrder(created.id(), updateReq)
        );
        assertTrue(ex.getMessage().contains("Completed orders cannot change status"));
    }
}
