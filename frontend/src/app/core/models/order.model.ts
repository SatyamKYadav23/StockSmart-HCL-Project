export type OrderStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED' | string;

/**
 * Backend OrderItemResponse DTO
 */
export interface OrderItemResponse {
  id?: number;
  productId: number;
  productName?: string;
  productSku?: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

/**
 * Backend OrderResponse DTO
 */
export interface OrderResponse {
  id: number;
  orderNumber: string;
  orderDate: string;
  status: OrderStatus;
  customerName?: string;
  locationId?: number;
  locationName?: string;
  totalAmount: number;
  remarks?: string;
  createdAt: string;
  items: OrderItemResponse[];
}

/**
 * Backend OrderItemRequest DTO
 */
export interface OrderItemRequest {
  productId: number;
  quantity: number;
  unitPrice: number;
}

/**
 * Backend OrderRequest DTO
 */
export interface OrderRequest {
  orderNumber: string;
  status: OrderStatus;
  customerName?: string;
  locationId?: number;
  totalAmount?: number;
  remarks?: string;
  items: OrderItemRequest[];
}

// Aliases for component usage
export type Order = OrderResponse;
export type OrderItem = OrderItemResponse;
