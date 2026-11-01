import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Product, ProductRequest, Category, Supplier } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/products
   * Fetches all products from Spring Boot backend.
   * No mock fallback — real backend data only.
   */
  getProducts(): Observable<Product[]> {
    return this.api.get<Product[]>('/products');
  }

  /**
   * GET /api/products/{id}
   * Fetches a single product by ID.
   */
  getProductById(id: number): Observable<Product> {
    return this.api.get<Product>(`/products/${id}`);
  }

  /**
   * POST /api/products
   * Creates a new product.
   */
  createProduct(request: ProductRequest): Observable<Product> {
    return this.api.post<Product>('/products', request);
  }

  /**
   * PUT /api/products/{id}
   * Updates an existing product.
   */
  updateProduct(id: number, request: ProductRequest): Observable<Product> {
    return this.api.put<Product>(`/products/${id}`, request);
  }

  /**
   * DELETE /api/products/{id}
   * Deletes a product by ID.
   */
  deleteProduct(id: number): Observable<void> {
    return this.api.delete<void>(`/products/${id}`);
  }

  /**
   * GET /api/categories
   * Fetches real categories from the database for product classification.
   */
  getCategories(): Observable<Category[]> {
    return this.api.get<Category[]>('/categories');
  }

  /**
   * POST /api/categories
   * Creates a category.
   */
  createCategory(category: { name: string; description?: string }): Observable<Category> {
    return this.api.post<Category>('/categories', category);
  }

  /**
   * GET /api/suppliers
   * Fetches registered suppliers for optional product supplier linking.
   */
  getSuppliers(): Observable<Supplier[]> {
    return this.api.get<Supplier[]>('/suppliers');
  }
}
