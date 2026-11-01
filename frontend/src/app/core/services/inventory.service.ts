import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { InventoryResponse, InventoryRequest, StockAdjustmentRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/inventory
   * Fetches real inventory stock records from Spring Boot backend.
   */
  getInventory(): Observable<InventoryResponse[]> {
    return this.api.get<InventoryResponse[]>('/inventory');
  }

  /**
   * GET /api/inventory/{id}
   */
  getInventoryById(id: number): Observable<InventoryResponse> {
    return this.api.get<InventoryResponse>(`/inventory/${id}`);
  }

  /**
   * POST /api/inventory/adjustment
   * Atomically adjusts stock and creates a StockMovement audit record in a single transaction.
   */
  adjustStock(request: StockAdjustmentRequest): Observable<InventoryResponse> {
    return this.api.post<InventoryResponse>('/inventory/adjustment', request);
  }

  /**
   * POST /api/inventory
   */
  createInventory(request: InventoryRequest): Observable<InventoryResponse> {
    return this.api.post<InventoryResponse>('/inventory', request);
  }

  /**
   * PUT /api/inventory/{id}
   */
  updateInventory(id: number, request: InventoryRequest): Observable<InventoryResponse> {
    return this.api.put<InventoryResponse>(`/inventory/${id}`, request);
  }

  /**
   * DELETE /api/inventory/{id}
   */
  deleteInventory(id: number): Observable<void> {
    return this.api.delete<void>(`/inventory/${id}`);
  }
}
