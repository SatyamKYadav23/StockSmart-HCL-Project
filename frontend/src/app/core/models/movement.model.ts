export type MovementType =
  | 'STOCK_IN'
  | 'STOCK_OUT'
  | 'TRANSFER'
  | 'ADJUSTMENT'
  | 'PURCHASE'
  | 'SALE'
  | 'RETURN'
  | string;

/**
 * Backend StockMovementResponse from GET /api/stock-movements
 */
export interface StockMovementResponse {
  id: number;
  productId: number;
  productName: string;
  productSku?: string;
  locationId: number;
  locationName: string;
  movementType: MovementType;
  direction?: 'IN' | 'OUT' | string;
  quantity: number;
  referenceType?: string;
  referenceId?: number;
  remarks?: string;
  createdAt: string;
}

/**
 * Movement presentation model for the table UI
 */
export interface StockMovement {
  id: number;
  referenceNumber: string;
  type: MovementType;
  direction?: 'IN' | 'OUT' | string;
  productId: number;
  productName: string;
  sku: string;
  quantity: number;
  fromLocationId?: number;
  fromLocationName?: string;
  toLocationId?: number;
  toLocationName?: string;
  locationName: string;
  timestamp: string;
  performedBy?: string;
  notes?: string;
}
