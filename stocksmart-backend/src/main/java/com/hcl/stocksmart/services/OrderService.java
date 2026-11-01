package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.OrderItemRequest;
import com.hcl.stocksmart.dto.OrderItemResponse;
import com.hcl.stocksmart.dto.OrderRequest;
import com.hcl.stocksmart.dto.OrderResponse;
import com.hcl.stocksmart.entity.*;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        if (orderRepository.existsByOrderNumber(request.orderNumber())) {
            throw new DuplicateResourceException(
                    "Order already exists with number: " + request.orderNumber()
            );
        }

        Location location = null;
        if (request.locationId() != null) {
            location = locationRepository.findById(request.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Location not found with id: " + request.locationId()
                    ));
        }

        // Calculate total amount from items if present
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        if (request.items() != null && !request.items().isEmpty()) {
            for (OrderItemRequest itemReq : request.items()) {
                if (itemReq.quantity() == null || itemReq.quantity() <= 0) {
                    throw new InvalidStockOperationException("Item quantity must be greater than zero");
                }
                BigDecimal subtotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
                calculatedTotal = calculatedTotal.add(subtotal);
            }
        } else if (request.totalAmount() != null) {
            calculatedTotal = request.totalAmount();
        }

        // 1. Create and save Order
        Order order = Order.builder()
                .orderNumber(request.orderNumber())
                .status(request.status())
                .customerName(request.customerName())
                .location(location)
                .totalAmount(calculatedTotal)
                .remarks(request.remarks())
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        // 2. Process Order Items and Inventory
        if (request.items() != null && !request.items().isEmpty()) {
            boolean isCompleted = "COMPLETED".equalsIgnoreCase(request.status());

            if (isCompleted && location == null) {
                throw new InvalidStockOperationException(
                        "Facility location is required to fulfill and deduct inventory for completed orders"
                );
            }

            for (OrderItemRequest itemReq : request.items()) {
                Product product = productRepository.findById(itemReq.productId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with id: " + itemReq.productId()
                        ));

                BigDecimal itemSubtotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));

                OrderItem orderItem = OrderItem.builder()
                        .order(savedOrder)
                        .product(product)
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .subtotal(itemSubtotal)
                        .build();

                OrderItem savedItem = orderItemRepository.save(orderItem);

                itemResponses.add(new OrderItemResponse(
                        savedItem.getId(),
                        product.getId(),
                        product.getName(),
                        product.getSku(),
                        savedItem.getQuantity(),
                        savedItem.getUnitPrice(),
                        savedItem.getSubtotal()
                ));

                // 3. Inventory Deduction & StockMovement Audit (if completed)
                if (isCompleted) {
                    final Location loc = location;
                    Inventory inventory = inventoryRepository.findByProductIdAndLocationIdWithLock(product.getId(), loc.getId())
                            .orElseThrow(() -> new InvalidStockOperationException(
                                    "No inventory record found for product '" + product.getName()
                                            + "' at " + loc.getName()
                            ));

                    if (inventory.getQuantity() < itemReq.quantity()) {
                        throw new InvalidStockOperationException(
                                "Insufficient stock for product '" + product.getName()
                                        + "' at " + loc.getName()
                                        + ". Available: " + inventory.getQuantity()
                                        + ", Requested: " + itemReq.quantity()
                        );
                    }

                    inventory.setQuantity(inventory.getQuantity() - itemReq.quantity());
                    inventoryRepository.save(inventory);

                    StockMovement movement = StockMovement.builder()
                            .product(product)
                            .location(loc)
                            .movementType("SALE")
                            .direction("OUT")
                            .quantity(itemReq.quantity())
                            .referenceType(savedOrder.getOrderNumber())
                            .remarks("Order fulfillment: " + savedOrder.getOrderNumber()
                                    + " (" + (savedOrder.getCustomerName() != null ? savedOrder.getCustomerName() : "Counter") + ")")
                            .build();

                    stockMovementRepository.save(movement);
                }
            }
        }

        return mapToResponse(savedOrder, itemResponses);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapOrderWithItems)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + id)
                );

        return mapOrderWithItems(order);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + id)
                );

        if (!order.getOrderNumber().equals(request.orderNumber())
                && orderRepository.existsByOrderNumber(request.orderNumber())) {
            throw new DuplicateResourceException(
                    "Order already exists with number: " + request.orderNumber()
            );
        }

        boolean wasCompleted = "COMPLETED".equalsIgnoreCase(order.getStatus());
        boolean willBeCompleted = "COMPLETED".equalsIgnoreCase(request.status());

        if (wasCompleted) {
            if (request.status() != null && !request.status().equalsIgnoreCase(order.getStatus())) {
                throw new InvalidStockOperationException(
                        "Completed orders cannot change status because inventory has already been deducted."
                );
            }
            if (request.locationId() != null && (order.getLocation() == null || !order.getLocation().getId().equals(request.locationId()))) {
                throw new InvalidStockOperationException(
                        "Cannot change facility location of a completed order."
                );
            }
        }

        Location location = null;
        if (request.locationId() != null) {
            location = locationRepository.findById(request.locationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Location not found with id: " + request.locationId()
                    ));
        } else if (order.getLocation() != null) {
            location = order.getLocation();
        }

        order.setOrderNumber(request.orderNumber());
        if (!wasCompleted) {
            order.setStatus(request.status());
        }
        order.setCustomerName(request.customerName());
        order.setLocation(location);
        if (request.totalAmount() != null) {
            order.setTotalAmount(request.totalAmount());
        }
        order.setRemarks(request.remarks());

        Order saved = orderRepository.save(order);

        // If transitioning from non-completed to COMPLETED, deduct inventory and record stock movements
        if (!wasCompleted && willBeCompleted) {
            if (location == null) {
                throw new InvalidStockOperationException(
                        "Facility location is required to fulfill and deduct inventory for completed orders"
                );
            }

            List<OrderItem> items = orderItemRepository.findByOrderId(saved.getId());
            final Location fulfillLocation = location;

            for (OrderItem item : items) {
                Product product = item.getProduct();
                Inventory inventory = inventoryRepository.findByProductIdAndLocationIdWithLock(product.getId(), fulfillLocation.getId())
                        .orElseThrow(() -> new InvalidStockOperationException(
                                "No inventory record found for product '" + product.getName()
                                        + "' at " + fulfillLocation.getName()
                        ));

                if (inventory.getQuantity() < item.getQuantity()) {
                    throw new InvalidStockOperationException(
                            "Insufficient stock for product '" + product.getName()
                                    + "' at " + fulfillLocation.getName()
                                    + ". Available: " + inventory.getQuantity()
                                    + ", Requested: " + item.getQuantity()
                    );
                }

                inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
                inventoryRepository.save(inventory);

                StockMovement movement = StockMovement.builder()
                        .product(product)
                        .location(fulfillLocation)
                        .movementType("SALE")
                        .direction("OUT")
                        .quantity(item.getQuantity())
                        .referenceType(saved.getOrderNumber())
                        .remarks("Order fulfillment: " + saved.getOrderNumber()
                                + " (" + (saved.getCustomerName() != null ? saved.getCustomerName() : "Counter") + ")")
                        .build();

                stockMovementRepository.save(movement);
            }
        }

        return mapOrderWithItems(saved);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + id)
                );

        if ("COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new InvalidStockOperationException(
                    "Completed orders cannot be deleted because inventory has already been deducted."
            );
        }

        orderItemRepository.deleteByOrderId(id);
        orderRepository.delete(order);
    }

    private OrderResponse mapOrderWithItems(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getSku(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return mapToResponse(order, itemResponses);
    }

    private OrderResponse mapToResponse(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderDate(),
                order.getStatus(),
                order.getCustomerName(),
                order.getLocation() != null ? order.getLocation().getId() : null,
                order.getLocation() != null ? order.getLocation().getName() : null,
                order.getTotalAmount(),
                order.getRemarks(),
                order.getCreatedAt(),
                items
        );
    }
}