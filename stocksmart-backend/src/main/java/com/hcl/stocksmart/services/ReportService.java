package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.report.*;
import com.hcl.stocksmart.entity.*;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PurchaseReceivingRepository purchaseReceivingRepository;
    private final PurchaseReceivingItemRepository purchaseReceivingItemRepository;
    private final StockTransferRepository stockTransferRepository;
    private final StockMovementRepository stockMovementRepository;

    /**
     * 1. Inventory Overview Report: Current-state stock snapshot & facility breakdown.
     */
    public InventoryOverviewReportResponse getInventoryOverview() {
        List<Product> products = productRepository.findAll();
        List<Location> locations = locationRepository.findAll();
        List<Inventory> allInventory = inventoryRepository.findAll();

        int totalActiveProducts = (int) products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .count();

        int totalUnitsOnHand = allInventory.stream()
                .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                .sum();

        int totalReservedUnits = allInventory.stream()
                .mapToInt(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0)
                .sum();

        int totalAvailableUnits = totalUnitsOnHand - totalReservedUnits;

        BigDecimal totalValuation = BigDecimal.ZERO;
        for (Inventory inv : allInventory) {
            Product p = inv.getProduct();
            if (p != null && p.getCostPrice() != null && p.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                int qty = inv.getQuantity() != null ? inv.getQuantity() : 0;
                totalValuation = totalValuation.add(p.getCostPrice().multiply(BigDecimal.valueOf(qty)));
            }
        }

        // Group inventory by location
        Map<Long, List<Inventory>> invByLoc = allInventory.stream()
                .filter(i -> i.getLocation() != null)
                .collect(Collectors.groupingBy(i -> i.getLocation().getId()));

        List<FacilityInventorySummaryDto> facilitySummaries = new ArrayList<>();
        for (Location loc : locations) {
            List<Inventory> locInvs = invByLoc.getOrDefault(loc.getId(), Collections.emptyList());

            int productsStocked = (int) locInvs.stream()
                    .filter(i -> i.getQuantity() != null && i.getQuantity() > 0)
                    .map(i -> i.getProduct().getId())
                    .distinct()
                    .count();

            int locUnits = locInvs.stream()
                    .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                    .sum();

            int locReserved = locInvs.stream()
                    .mapToInt(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0)
                    .sum();

            int locAvailable = locUnits - locReserved;

            BigDecimal locValuation = BigDecimal.ZERO;
            for (Inventory inv : locInvs) {
                Product p = inv.getProduct();
                if (p != null && p.getCostPrice() != null && p.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                    int q = inv.getQuantity() != null ? inv.getQuantity() : 0;
                    locValuation = locValuation.add(p.getCostPrice().multiply(BigDecimal.valueOf(q)));
                }
            }

            facilitySummaries.add(new FacilityInventorySummaryDto(
                    loc.getId(),
                    loc.getName(),
                    loc.getLocationType(),
                    productsStocked,
                    locUnits,
                    locReserved,
                    locAvailable,
                    locValuation
            ));
        }

        // Sort facility summaries by name
        facilitySummaries.sort(Comparator.comparing(FacilityInventorySummaryDto::locationName, String.CASE_INSENSITIVE_ORDER));

        return new InventoryOverviewReportResponse(
                totalActiveProducts,
                totalUnitsOnHand,
                totalReservedUnits,
                totalAvailableUnits,
                locations.size(),
                totalValuation,
                facilitySummaries
        );
    }

    /**
     * 2. Stock Status Report: Product stock levels vs reorder thresholds.
     */
    public StockStatusReportResponse getStockStatus(Long locationId, Long categoryId, Long productId, String statusFilter) {
        List<Inventory> allInventory = inventoryRepository.findAll();
        List<Product> allProducts = productRepository.findAll();

        // Filter inventory records
        List<Inventory> filteredInventory = allInventory.stream()
                .filter(i -> locationId == null || (i.getLocation() != null && i.getLocation().getId().equals(locationId)))
                .filter(i -> productId == null || (i.getProduct() != null && i.getProduct().getId().equals(productId)))
                .filter(i -> categoryId == null || (i.getProduct() != null && i.getProduct().getCategory() != null && i.getProduct().getCategory().getId().equals(categoryId)))
                .toList();

        List<StockStatusItemDto> items = new ArrayList<>();
        int inStockCount = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;
        int uninitializedCount = 0;

        for (Inventory inv : filteredInventory) {
            Product p = inv.getProduct();
            Location l = inv.getLocation();
            if (p == null || l == null) continue;

            int onHand = inv.getQuantity() != null ? inv.getQuantity() : 0;
            int reserved = inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0;
            int available = onHand - reserved;
            int reorder = p.getReorderLevel() != null ? p.getReorderLevel() : 0;

            String status;
            if (onHand <= 0) {
                status = "OUT_OF_STOCK";
                outOfStockCount++;
            } else if (onHand <= reorder) {
                status = "LOW_STOCK";
                lowStockCount++;
            } else {
                status = "IN_STOCK";
                inStockCount++;
            }

            if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)
                    || status.equalsIgnoreCase(statusFilter)) {
                items.add(new StockStatusItemDto(
                        p.getId(),
                        p.getSku(),
                        p.getName(),
                        p.getCategory() != null ? p.getCategory().getId() : null,
                        p.getCategory() != null ? p.getCategory().getName() : "Unassigned",
                        l.getId(),
                        l.getName(),
                        onHand,
                        reserved,
                        available,
                        reorder,
                        status,
                        true
                ));
            }
        }

        // If a specific location is selected, surface uninitialized products at that location
        if (locationId != null) {
            Set<Long> initializedProductIds = filteredInventory.stream()
                    .map(i -> i.getProduct().getId())
                    .collect(Collectors.toSet());

            Location targetLoc = locationRepository.findById(locationId).orElse(null);
            String locName = targetLoc != null ? targetLoc.getName() : "Facility #" + locationId;

            for (Product p : allProducts) {
                if (!Boolean.TRUE.equals(p.getActive())) continue;
                if (productId != null && !p.getId().equals(productId)) continue;
                if (categoryId != null && (p.getCategory() == null || !p.getCategory().getId().equals(categoryId))) continue;

                if (!initializedProductIds.contains(p.getId())) {
                    uninitializedCount++;
                    if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)
                            || "NOT_STOCKED".equalsIgnoreCase(statusFilter) || "UNINITIALIZED".equalsIgnoreCase(statusFilter)) {
                        items.add(new StockStatusItemDto(
                                p.getId(),
                                p.getSku(),
                                p.getName(),
                                p.getCategory() != null ? p.getCategory().getId() : null,
                                p.getCategory() != null ? p.getCategory().getName() : "Unassigned",
                                locationId,
                                locName,
                                0,
                                0,
                                0,
                                p.getReorderLevel() != null ? p.getReorderLevel() : 0,
                                "NOT_STOCKED",
                                false
                        ));
                    }
                }
            }
        }

        // Sort items: OUT_OF_STOCK first, then LOW_STOCK, then IN_STOCK, then NOT_STOCKED
        Map<String, Integer> orderMap = Map.of(
                "OUT_OF_STOCK", 1,
                "LOW_STOCK", 2,
                "IN_STOCK", 3,
                "NOT_STOCKED", 4
        );
        items.sort(Comparator.comparingInt((StockStatusItemDto i) -> orderMap.getOrDefault(i.status(), 99))
                .thenComparing(StockStatusItemDto::productName, String.CASE_INSENSITIVE_ORDER));

        return new StockStatusReportResponse(
                items.size(),
                inStockCount,
                lowStockCount,
                outOfStockCount,
                uninitializedCount,
                items
        );
    }

    /**
     * 3. Inventory Valuation Report: Quantity * costPrice aggregation with missing-cost flagging.
     */
    public InventoryValuationReportResponse getInventoryValuation(Long locationId, Long categoryId) {
        List<Inventory> allInventory = inventoryRepository.findAll();

        List<Inventory> filtered = allInventory.stream()
                .filter(i -> locationId == null || (i.getLocation() != null && i.getLocation().getId().equals(locationId)))
                .filter(i -> categoryId == null || (i.getProduct() != null && i.getProduct().getCategory() != null && i.getProduct().getCategory().getId().equals(categoryId)))
                .toList();

        BigDecimal grandValuation = BigDecimal.ZERO;
        int grandUnits = 0;
        Set<Long> productsMissingCost = new HashSet<>();

        List<ProductValuationDto> itemDtos = new ArrayList<>();

        for (Inventory inv : filtered) {
            Product p = inv.getProduct();
            Location l = inv.getLocation();
            if (p == null || l == null) continue;

            int qty = inv.getQuantity() != null ? inv.getQuantity() : 0;
            grandUnits += qty;

            BigDecimal cost = p.getCostPrice();
            boolean isMissing = (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0);

            BigDecimal lineValuation = BigDecimal.ZERO;
            if (!isMissing) {
                lineValuation = cost.multiply(BigDecimal.valueOf(qty));
                grandValuation = grandValuation.add(lineValuation);
            } else {
                productsMissingCost.add(p.getId());
            }

            itemDtos.add(new ProductValuationDto(
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    p.getCategory() != null ? p.getCategory().getId() : null,
                    p.getCategory() != null ? p.getCategory().getName() : "Unassigned",
                    l.getId(),
                    l.getName(),
                    qty,
                    cost,
                    lineValuation,
                    isMissing
            ));
        }

        // Facility breakdown
        Map<Long, List<ProductValuationDto>> byLoc = itemDtos.stream()
                .collect(Collectors.groupingBy(ProductValuationDto::locationId));

        List<FacilityValuationDto> facilityBreakdown = new ArrayList<>();
        for (Map.Entry<Long, List<ProductValuationDto>> entry : byLoc.entrySet()) {
            List<ProductValuationDto> list = entry.getValue();
            String locName = list.get(0).locationName();
            int totalUnits = list.stream().mapToInt(ProductValuationDto::quantity).sum();
            BigDecimal locVal = list.stream().map(ProductValuationDto::valuation).reduce(BigDecimal.ZERO, BigDecimal::add);
            int missingCount = (int) list.stream().filter(ProductValuationDto::missingCost).map(ProductValuationDto::productId).distinct().count();

            facilityBreakdown.add(new FacilityValuationDto(entry.getKey(), locName, totalUnits, locVal, missingCount));
        }
        facilityBreakdown.sort(Comparator.comparing(FacilityValuationDto::locationName, String.CASE_INSENSITIVE_ORDER));

        // Category breakdown
        Map<String, List<ProductValuationDto>> byCat = itemDtos.stream()
                .collect(Collectors.groupingBy(ProductValuationDto::categoryName));

        List<CategoryValuationDto> categoryBreakdown = new ArrayList<>();
        for (Map.Entry<String, List<ProductValuationDto>> entry : byCat.entrySet()) {
            List<ProductValuationDto> list = entry.getValue();
            Long catId = list.get(0).categoryId();
            int totalUnits = list.stream().mapToInt(ProductValuationDto::quantity).sum();
            BigDecimal catVal = list.stream().map(ProductValuationDto::valuation).reduce(BigDecimal.ZERO, BigDecimal::add);

            categoryBreakdown.add(new CategoryValuationDto(catId, entry.getKey(), totalUnits, catVal));
        }
        categoryBreakdown.sort(Comparator.comparing(CategoryValuationDto::categoryName, String.CASE_INSENSITIVE_ORDER));

        // Sort items by valuation descending
        itemDtos.sort(Comparator.comparing(ProductValuationDto::valuation).reversed()
                .thenComparing(ProductValuationDto::productName, String.CASE_INSENSITIVE_ORDER));

        return new InventoryValuationReportResponse(
                grandValuation,
                grandUnits,
                productsMissingCost.size(),
                facilityBreakdown,
                categoryBreakdown,
                itemDtos
        );
    }

    /**
     * 4. Stock Movement Report: Audit movements with signed quantities and net balance.
     */
    public StockMovementReportResponse getStockMovements(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long productId,
            Long locationId,
            String movementType,
            String reference
    ) {
        validateDateRange(startDate, endDate);

        List<StockMovement> movements = stockMovementRepository.findMovementsFiltered(
                startDate,
                endDate,
                productId,
                locationId,
                (movementType != null && !movementType.isBlank() && !"ALL".equalsIgnoreCase(movementType)) ? movementType : null,
                (reference != null && !reference.isBlank()) ? reference.trim() : null
        );

        List<StockMovementReportItemDto> items = new ArrayList<>();
        int totalInflowUnits = 0;
        int totalOutflowUnits = 0;

        for (StockMovement m : movements) {
            int qty = m.getQuantity() != null ? m.getQuantity() : 0;
            String type = m.getMovementType() != null ? m.getMovementType().toUpperCase() : "ADJUSTMENT";
            String remarks = m.getRemarks() != null ? m.getRemarks() : "";

            String direction = m.getEffectiveDirection();
            int signedQuantity;

            if ("OUT".equalsIgnoreCase(direction)) {
                direction = "OUT";
                signedQuantity = -qty;
                totalOutflowUnits += qty;
            } else {
                direction = "IN";
                signedQuantity = qty;
                totalInflowUnits += qty;
            }

            items.add(new StockMovementReportItemDto(
                    m.getId(),
                    m.getCreatedAt(),
                    m.getReferenceType(),
                    m.getReferenceId(),
                    m.getProduct() != null ? m.getProduct().getId() : null,
                    m.getProduct() != null ? m.getProduct().getName() : "Unknown Product",
                    m.getProduct() != null ? m.getProduct().getSku() : "N/A",
                    m.getLocation() != null ? m.getLocation().getId() : null,
                    m.getLocation() != null ? m.getLocation().getName() : "Unknown Location",
                    type,
                    direction,
                    qty,
                    signedQuantity,
                    remarks
            ));
        }

        int netDelta = totalInflowUnits - totalOutflowUnits;

        return new StockMovementReportResponse(
                items.size(),
                totalInflowUnits,
                totalOutflowUnits,
                netDelta,
                items
        );
    }

    /**
     * 5. Order Summary Report: Order performance, fulfillment values, and status counts.
     */
    public OrderSummaryReportResponse getOrderSummary(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String status,
            Long locationId
    ) {
        validateDateRange(startDate, endDate);

        String normalizedStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? status.trim().toUpperCase()
                : null;

        List<Order> orders = orderRepository.findOrdersFiltered(startDate, endDate, normalizedStatus, locationId);

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
            itemsByOrderId = allItems.stream().collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        }

        int completedCount = 0;
        int pendingCount = 0;
        int cancelledCount = 0;
        int totalQuantity = 0;
        BigDecimal completedValue = BigDecimal.ZERO;
        BigDecimal grandTotalValue = BigDecimal.ZERO;

        List<OrderSummaryItemDto> items = new ArrayList<>();

        for (Order order : orders) {
            String ordStatus = order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING";
            BigDecimal amt = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            grandTotalValue = grandTotalValue.add(amt);

            if ("COMPLETED".equalsIgnoreCase(ordStatus)) {
                completedCount++;
                completedValue = completedValue.add(amt);
            } else if ("CANCELLED".equalsIgnoreCase(ordStatus)) {
                cancelledCount++;
            } else {
                pendingCount++;
            }

            List<OrderItem> orderItems = itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList());
            int orderQty = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
            totalQuantity += orderQty;

            items.add(new OrderSummaryItemDto(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getOrderDate(),
                    order.getCustomerName() != null ? order.getCustomerName() : "Walk-in Customer",
                    order.getLocation() != null ? order.getLocation().getId() : null,
                    order.getLocation() != null ? order.getLocation().getName() : "Unassigned",
                    ordStatus,
                    orderItems.size(),
                    orderQty,
                    amt
            ));
        }

        return new OrderSummaryReportResponse(
                orders.size(),
                completedCount,
                pendingCount,
                cancelledCount,
                totalQuantity,
                completedValue,
                grandTotalValue,
                items
        );
    }

    /**
     * 6. Purchase Receiving Report: Receipts, supplier breakdown, excluding cancelled receipts from totals.
     */
    public PurchaseReceivingReportResponse getPurchaseReceivingReport(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long supplierId,
            Long locationId,
            String status
    ) {
        validateDateRange(startDate, endDate);

        String normalizedStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? status.trim().toUpperCase()
                : null;

        List<PurchaseReceiving> receivings = purchaseReceivingRepository.findReceivingsFiltered(
                startDate,
                endDate,
                supplierId,
                locationId,
                normalizedStatus
        );

        List<Long> receivingIds = receivings.stream().map(PurchaseReceiving::getId).toList();
        Map<Long, List<PurchaseReceivingItem>> itemsByReceivingId = new HashMap<>();
        if (!receivingIds.isEmpty()) {
            List<PurchaseReceivingItem> allItems = purchaseReceivingItemRepository.findByReceivingIdIn(receivingIds);
            itemsByReceivingId = allItems.stream().collect(Collectors.groupingBy(i -> i.getReceiving().getId()));
        }

        int completedReceipts = 0;
        int cancelledReceipts = 0;
        int totalReceivedQuantity = 0;
        BigDecimal totalReceivedValue = BigDecimal.ZERO;

        Map<Long, SupplierReceivingSummaryDto> supplierMap = new HashMap<>();
        Map<Long, FacilityReceivingSummaryDto> facilityMap = new HashMap<>();
        List<PurchaseReceivingReportItemDto> items = new ArrayList<>();

        for (PurchaseReceiving pr : receivings) {
            String recStatus = pr.getStatus() != null ? pr.getStatus().toUpperCase() : "RECEIVED";
            boolean isCompleted = "RECEIVED".equalsIgnoreCase(recStatus);
            boolean isCancelled = "CANCELLED".equalsIgnoreCase(recStatus);

            List<PurchaseReceivingItem> prItems = itemsByReceivingId.getOrDefault(pr.getId(), Collections.emptyList());
            int prQty = prItems.stream().mapToInt(PurchaseReceivingItem::getQuantity).sum();
            BigDecimal prAmount = pr.getTotalAmount() != null ? pr.getTotalAmount() : BigDecimal.ZERO;

            if (isCompleted) {
                completedReceipts++;
                totalReceivedQuantity += prQty;
                totalReceivedValue = totalReceivedValue.add(prAmount);

                // Supplier breakdown (only for completed receipts)
                if (pr.getSupplier() != null) {
                    Long supId = pr.getSupplier().getId();
                    String supName = pr.getSupplier().getName();
                    SupplierReceivingSummaryDto existing = supplierMap.getOrDefault(
                            supId,
                            new SupplierReceivingSummaryDto(supId, supName, 0, 0, BigDecimal.ZERO)
                    );
                    supplierMap.put(supId, new SupplierReceivingSummaryDto(
                            supId,
                            supName,
                            existing.receiptsCount() + 1,
                            existing.receivedUnits() + prQty,
                            existing.totalAmount().add(prAmount)
                    ));
                }

                // Facility breakdown (only for completed receipts)
                if (pr.getLocation() != null) {
                    Long locId = pr.getLocation().getId();
                    String locName = pr.getLocation().getName();
                    FacilityReceivingSummaryDto existing = facilityMap.getOrDefault(
                            locId,
                            new FacilityReceivingSummaryDto(locId, locName, 0, 0, BigDecimal.ZERO)
                    );
                    facilityMap.put(locId, new FacilityReceivingSummaryDto(
                            locId,
                            locName,
                            existing.receiptsCount() + 1,
                            existing.receivedUnits() + prQty,
                            existing.totalAmount().add(prAmount)
                    ));
                }
            } else if (isCancelled) {
                cancelledReceipts++;
            }

            items.add(new PurchaseReceivingReportItemDto(
                    pr.getId(),
                    pr.getReceivingNumber(),
                    pr.getReceivingDate(),
                    pr.getSupplier() != null ? pr.getSupplier().getId() : null,
                    pr.getSupplier() != null ? pr.getSupplier().getName() : "Unknown Supplier",
                    pr.getLocation() != null ? pr.getLocation().getId() : null,
                    pr.getLocation() != null ? pr.getLocation().getName() : "Unknown Location",
                    recStatus,
                    prItems.size(),
                    prQty,
                    prAmount,
                    pr.getRemarks()
            ));
        }

        List<SupplierReceivingSummaryDto> supplierBreakdown = new ArrayList<>(supplierMap.values());
        supplierBreakdown.sort(Comparator.comparing(SupplierReceivingSummaryDto::supplierName, String.CASE_INSENSITIVE_ORDER));

        List<FacilityReceivingSummaryDto> facilityBreakdown = new ArrayList<>(facilityMap.values());
        facilityBreakdown.sort(Comparator.comparing(FacilityReceivingSummaryDto::locationName, String.CASE_INSENSITIVE_ORDER));

        return new PurchaseReceivingReportResponse(
                receivings.size(),
                completedReceipts,
                cancelledReceipts,
                totalReceivedQuantity,
                totalReceivedValue,
                supplierBreakdown,
                facilityBreakdown,
                items
        );
    }

    /**
     * 7. Stock Transfer Report: Stock relocation between facilities, excluding cancelled transfers from totals.
     */
    public StockTransferReportResponse getStockTransferReport(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long productId,
            Long sourceLocationId,
            Long destinationLocationId,
            String status
    ) {
        validateDateRange(startDate, endDate);

        StockTransferStatus transferStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                transferStatus = StockTransferStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidStockOperationException("Invalid transfer status filter: " + status);
            }
        }

        List<StockTransfer> transfers = stockTransferRepository.findTransfersFiltered(
                startDate,
                endDate,
                productId,
                sourceLocationId,
                destinationLocationId,
                transferStatus
        );

        int completedTransfers = 0;
        int cancelledTransfers = 0;
        int totalUnitsRelocated = 0;

        Map<Long, FacilityTransferSummaryDto> sourceMap = new HashMap<>();
        Map<Long, FacilityTransferSummaryDto> destMap = new HashMap<>();
        Map<Long, ProductTransferSummaryDto> productMap = new HashMap<>();

        List<StockTransferReportItemDto> items = new ArrayList<>();

        for (StockTransfer st : transfers) {
            boolean isCompleted = StockTransferStatus.COMPLETED.equals(st.getStatus());
            boolean isCancelled = StockTransferStatus.CANCELLED.equals(st.getStatus());
            int qty = st.getQuantity() != null ? st.getQuantity() : 0;

            if (isCompleted) {
                completedTransfers++;
                totalUnitsRelocated += qty;

                // Source breakdown
                if (st.getSourceLocation() != null) {
                    Long sId = st.getSourceLocation().getId();
                    String sName = st.getSourceLocation().getName();
                    FacilityTransferSummaryDto existing = sourceMap.getOrDefault(
                            sId,
                            new FacilityTransferSummaryDto(sId, sName, 0, 0)
                    );
                    sourceMap.put(sId, new FacilityTransferSummaryDto(
                            sId,
                            sName,
                            existing.transferCount() + 1,
                            existing.units() + qty
                    ));
                }

                // Destination breakdown
                if (st.getDestinationLocation() != null) {
                    Long dId = st.getDestinationLocation().getId();
                    String dName = st.getDestinationLocation().getName();
                    FacilityTransferSummaryDto existing = destMap.getOrDefault(
                            dId,
                            new FacilityTransferSummaryDto(dId, dName, 0, 0)
                    );
                    destMap.put(dId, new FacilityTransferSummaryDto(
                            dId,
                            dName,
                            existing.transferCount() + 1,
                            existing.units() + qty
                    ));
                }

                // Product breakdown
                if (st.getProduct() != null) {
                    Long pId = st.getProduct().getId();
                    String pName = st.getProduct().getName();
                    String sku = st.getProduct().getSku();
                    ProductTransferSummaryDto existing = productMap.getOrDefault(
                            pId,
                            new ProductTransferSummaryDto(pId, pName, sku, 0, 0)
                    );
                    productMap.put(pId, new ProductTransferSummaryDto(
                            pId,
                            pName,
                            sku,
                            existing.transferCount() + 1,
                            existing.unitsRelocated() + qty
                    ));
                }
            } else if (isCancelled) {
                cancelledTransfers++;
            }

            items.add(new StockTransferReportItemDto(
                    st.getId(),
                    st.getTransferNumber(),
                    st.getTransferDate(),
                    st.getProduct() != null ? st.getProduct().getId() : null,
                    st.getProduct() != null ? st.getProduct().getName() : "Unknown Product",
                    st.getProduct() != null ? st.getProduct().getSku() : "N/A",
                    st.getSourceLocation() != null ? st.getSourceLocation().getId() : null,
                    st.getSourceLocation() != null ? st.getSourceLocation().getName() : "Unknown Source",
                    st.getDestinationLocation() != null ? st.getDestinationLocation().getId() : null,
                    st.getDestinationLocation() != null ? st.getDestinationLocation().getName() : "Unknown Destination",
                    qty,
                    st.getStatus() != null ? st.getStatus().name() : "COMPLETED",
                    st.getRemarks()
            ));
        }

        List<FacilityTransferSummaryDto> sourceBreakdown = new ArrayList<>(sourceMap.values());
        sourceBreakdown.sort(Comparator.comparing(FacilityTransferSummaryDto::locationName, String.CASE_INSENSITIVE_ORDER));

        List<FacilityTransferSummaryDto> destBreakdown = new ArrayList<>(destMap.values());
        destBreakdown.sort(Comparator.comparing(FacilityTransferSummaryDto::locationName, String.CASE_INSENSITIVE_ORDER));

        List<ProductTransferSummaryDto> productBreakdown = new ArrayList<>(productMap.values());
        productBreakdown.sort(Comparator.comparing(ProductTransferSummaryDto::productName, String.CASE_INSENSITIVE_ORDER));

        return new StockTransferReportResponse(
                transfers.size(),
                completedTransfers,
                cancelledTransfers,
                totalUnitsRelocated,
                sourceBreakdown,
                destBreakdown,
                productBreakdown,
                items
        );
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidStockOperationException("Start date cannot be after end date");
        }
    }
}
