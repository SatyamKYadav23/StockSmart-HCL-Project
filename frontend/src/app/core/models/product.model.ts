/**
 * ProductResponse model representing the Spring Boot Product entity response DTO
 */
export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  categoryId: number;
  categoryName: string;
  supplierId: number | null;
  supplierName: string | null;
  price: number;
  costPrice: number | null;
  barcode: string | null;
  reorderLevel: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * ProductRequest model representing the Spring Boot Product creation and update DTO
 */
export interface ProductRequest {
  sku: string;
  name: string;
  description?: string | null;
  categoryId: number;
  supplierId?: number | null;
  price: number;
  costPrice?: number | null;
  barcode?: string | null;
  reorderLevel?: number;
}
