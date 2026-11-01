import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Category, CategoryRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/categories
   * Fetches all product categories.
   */
  getCategories(): Observable<Category[]> {
    return this.api.get<Category[]>('/categories');
  }

  /**
   * GET /api/categories/{id}
   * Fetches a single category by primary key.
   */
  getCategoryById(id: number): Observable<Category> {
    return this.api.get<Category>(`/categories/${id}`);
  }

  /**
   * POST /api/categories
   * Registers a new product category.
   */
  createCategory(request: CategoryRequest): Observable<Category> {
    return this.api.post<Category>('/categories', request);
  }

  /**
   * PUT /api/categories/{id}
   * Updates an existing product category.
   */
  updateCategory(id: number, request: CategoryRequest): Observable<Category> {
    return this.api.put<Category>(`/categories/${id}`, request);
  }

  /**
   * DELETE /api/categories/{id}
   * Deletes a category by primary key.
   */
  deleteCategory(id: number): Observable<void> {
    return this.api.delete<void>(`/categories/${id}`);
  }
}
