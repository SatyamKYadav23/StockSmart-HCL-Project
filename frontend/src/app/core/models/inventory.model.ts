/**
 * Raw backend InventoryResponse from GET /api/inventory
 */
export interface InventoryResponse {
  id: number;
  productId: number;
  productName: string;
  locationId: number;
  locationName: string;
  quantity: number;
  reservedQuantity: number;
  updatedAt: string;
}

/**
 * Request payload for POST / PUT /api/inventory
 */
export interface InventoryRequest {
  productId: number;
  locationId: number;
  quantity: number;
  reservedQuantity?: number;
}

/**
 * Request payload for POST /api/inventory/adjustment
 */
export interface StockAdjustmentRequest {
  productId: number;
  locationId: number;
  quantityChange: number;
  referenceNumber?: string;
  notes?: string;
}

export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';

/**
 * Enriched InventoryItem for table rendering with derived values
 */
export interface InventoryItem extends InventoryResponse {
  sku?: string;
  reorderLevel?: number;
  availableQuantity: number;
  status: StockStatus;
}

export interface StockAlert {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  locationId: number;
  locationName: string;
  currentStock: number;
  reorderLevel: number;
  deficit: number;
  status: 'LOW_STOCK' | 'CRITICAL' | 'OUT_OF_STOCK';
}
