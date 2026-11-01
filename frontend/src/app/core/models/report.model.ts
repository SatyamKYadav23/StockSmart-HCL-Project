export interface FacilityInventorySummary {
  locationId: number;
  locationName: string;
  locationType: string;
  productsStocked: number;
  unitsOnHand: number;
  reservedUnits: number;
  availableUnits: number;
  valuation: number;
}

export interface InventoryOverviewReportResponse {
  totalActiveProducts: number;
  totalUnitsOnHand: number;
  totalReservedUnits: number;
  totalAvailableUnits: number;
  totalFacilities: number;
  totalValuation: number;
  facilitySummaries: FacilityInventorySummary[];
}

export interface StockStatusItem {
  productId: number;
  sku: string;
  productName: string;
  categoryId?: number;
  categoryName?: string;
  locationId: number;
  locationName: string;
  onHand: number;
  reserved: number;
  available: number;
  reorderLevel: number;
  status: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK' | 'NOT_STOCKED' | string;
  initialized: boolean;
}

export interface StockStatusReportResponse {
  totalItems: number;
  inStockCount: number;
  lowStockCount: number;
  outOfStockCount: number;
  uninitializedCount: number;
  items: StockStatusItem[];
}

export interface FacilityValuation {
  locationId: number;
  locationName: string;
  totalUnits: number;
  valuation: number;
  missingCostCount: number;
}

export interface CategoryValuation {
  categoryId?: number;
  categoryName: string;
  totalUnits: number;
  valuation: number;
}

export interface ProductValuation {
  productId: number;
  sku: string;
  productName: string;
  categoryId?: number;
  categoryName: string;
  locationId: number;
  locationName: string;
  quantity: number;
  costPrice: number | null;
  valuation: number;
  missingCost: boolean;
}

export interface InventoryValuationReportResponse {
  totalValuation: number;
  totalUnitsOnHand: number;
  productsWithMissingCost: number;
  facilityBreakdown: FacilityValuation[];
  categoryBreakdown: CategoryValuation[];
  items: ProductValuation[];
}

export interface StockMovementReportItem {
  id: number;
  createdAt: string;
  referenceType?: string;
  referenceId?: number;
  productId: number;
  productName: string;
  sku: string;
  locationId: number;
  locationName: string;
  movementType: string;
  direction: 'IN' | 'OUT';
  quantity: number;
  signedQuantity: number;
  remarks: string;
}

export interface StockMovementReportResponse {
  totalMovements: number;
  totalInflowUnits: number;
  totalOutflowUnits: number;
  netDeltaUnits: number;
  items: StockMovementReportItem[];
}

export interface OrderSummaryItem {
  id: number;
  orderNumber: string;
  orderDate: string;
  customerName: string;
  locationId?: number;
  locationName?: string;
  status: string;
  itemCount: number;
  totalQuantity: number;
  orderValue: number;
}

export interface OrderSummaryReportResponse {
  totalOrders: number;
  completedOrders: number;
  pendingOrders: number;
  cancelledOrders: number;
  totalQuantity: number;
  completedOrderValue: number;
  totalOrderValue: number;
  items: OrderSummaryItem[];
}

export interface SupplierReceivingSummary {
  supplierId: number;
  supplierName: string;
  receiptsCount: number;
  receivedUnits: number;
  totalAmount: number;
}

export interface FacilityReceivingSummary {
  locationId: number;
  locationName: string;
  receiptsCount: number;
  receivedUnits: number;
  totalAmount: number;
}

export interface PurchaseReceivingReportItem {
  id: number;
  receivingNumber: string;
  receivingDate: string;
  supplierId: number;
  supplierName: string;
  locationId: number;
  locationName: string;
  status: string;
  itemCount: number;
  receivedQuantity: number;
  totalAmount: number;
  remarks?: string;
}

export interface PurchaseReceivingReportResponse {
  totalReceipts: number;
  completedReceipts: number;
  cancelledReceipts: number;
  receivedQuantity: number;
  receivedValue: number;
  supplierBreakdown: SupplierReceivingSummary[];
  facilityBreakdown: FacilityReceivingSummary[];
  items: PurchaseReceivingReportItem[];
}

export interface FacilityTransferSummary {
  locationId: number;
  locationName: string;
  transferCount: number;
  units: number;
}

export interface ProductTransferSummary {
  productId: number;
  productName: string;
  sku: string;
  transferCount: number;
  unitsRelocated: number;
}

export interface StockTransferReportItem {
  id: number;
  transferNumber: string;
  transferDate: string;
  productId: number;
  productName: string;
  sku: string;
  sourceLocationId: number;
  sourceLocationName: string;
  destinationLocationId: number;
  destinationLocationName: string;
  quantity: number;
  status: string;
  remarks?: string;
}

export interface StockTransferReportResponse {
  totalTransfers: number;
  completedTransfers: number;
  cancelledTransfers: number;
  totalUnitsRelocated: number;
  sourceBreakdown: FacilityTransferSummary[];
  destinationBreakdown: FacilityTransferSummary[];
  productBreakdown: ProductTransferSummary[];
  items: StockTransferReportItem[];
}

export type ReportCategory =
  | 'inventory-overview'
  | 'stock-status'
  | 'inventory-valuation'
  | 'stock-movement'
  | 'order-summary'
  | 'purchase-receiving'
  | 'stock-transfers';

export type DateFilterPreset = 'all' | 'today' | 'last7' | 'last30' | 'thisMonth' | 'custom';
