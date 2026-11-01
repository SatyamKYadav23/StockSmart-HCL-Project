export type PurchaseReceivingStatus = 'RECEIVED' | 'PENDING' | 'CANCELLED' | string;

/**
 * Backend PurchaseReceivingItemRequest DTO
 */
export interface PurchaseReceivingItemRequest {
  productId: number;
  quantity: number;
  unitCost: number;
}

/**
 * Backend PurchaseReceivingRequest DTO
 */
export interface PurchaseReceivingRequest {
  receivingNumber: string;
  receivingDate?: string;
  supplierId: number;
  locationId: number;
  status?: string;
  totalAmount?: number;
  remarks?: string;
  items: PurchaseReceivingItemRequest[];
}

/**
 * Backend PurchaseReceivingItemResponse DTO
 */
export interface PurchaseReceivingItemResponse {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  quantity: number;
  unitCost: number;
  subtotal: number;
}

/**
 * Backend PurchaseReceivingResponse DTO
 */
export interface PurchaseReceivingResponse {
  id: number;
  receivingNumber: string;
  receivingDate: string;
  supplierId: number;
  supplierName: string;
  locationId: number;
  locationName: string;
  status: PurchaseReceivingStatus;
  totalAmount: number;
  remarks?: string;
  createdAt: string;
  items: PurchaseReceivingItemResponse[];
}

// Aliases for convenience
export type PurchaseReceiving = PurchaseReceivingResponse;
export type PurchaseReceivingItem = PurchaseReceivingItemResponse;
