export type StockTransferStatus = 'COMPLETED' | 'CANCELLED' | string;

/**
 * Request payload for creating an inter-facility stock transfer
 */
export interface StockTransferRequest {
  transferNumber: string;
  transferDate?: string;
  productId: number;
  sourceLocationId: number;
  destinationLocationId: number;
  quantity: number;
  status?: StockTransferStatus;
  remarks?: string;
}

/**
 * Response payload representing an inter-facility stock transfer
 */
export interface StockTransferResponse {
  id: number;
  transferNumber: string;
  transferDate: string;
  productId: number;
  productName: string;
  productSku: string;
  sourceLocationId: number;
  sourceLocationName: string;
  destinationLocationId: number;
  destinationLocationName: string;
  quantity: number;
  status: StockTransferStatus;
  remarks?: string;
  createdAt: string;
}

// Aliases for convenience
export type StockTransfer = StockTransferResponse;
