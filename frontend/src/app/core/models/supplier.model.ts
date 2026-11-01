/**
 * Backend SupplierResponse DTO from GET /api/suppliers
 */
export interface SupplierResponse {
  id: number;
  name: string;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  createdAt: string;
}

/**
 * Backend SupplierRequest DTO for POST / PUT /api/suppliers
 */
export interface SupplierRequest {
  name: string;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
}

// Model alias for general component consumption
export type Supplier = SupplierResponse;

/**
 * Enriched representation of Supplier with associated products count
 */
export interface SupplierWithProducts extends SupplierResponse {
  productsCount: number;
}
