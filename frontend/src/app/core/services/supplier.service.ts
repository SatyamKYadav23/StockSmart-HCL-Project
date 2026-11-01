import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { SupplierResponse, SupplierRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class SupplierService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/suppliers
   * Fetches all registered suppliers from the database.
   */
  getSuppliers(): Observable<SupplierResponse[]> {
    return this.api.get<SupplierResponse[]>('/suppliers');
  }

  /**
   * GET /api/suppliers/{id}
   * Fetches a single supplier by ID.
   */
  getSupplierById(id: number): Observable<SupplierResponse> {
    return this.api.get<SupplierResponse>(`/suppliers/${id}`);
  }

  /**
   * POST /api/suppliers
   * Registers a new supplier.
   */
  createSupplier(request: SupplierRequest): Observable<SupplierResponse> {
    return this.api.post<SupplierResponse>('/suppliers', request);
  }

  /**
   * PUT /api/suppliers/{id}
   * Updates an existing supplier's details.
   */
  updateSupplier(id: number, request: SupplierRequest): Observable<SupplierResponse> {
    return this.api.put<SupplierResponse>(`/suppliers/${id}`, request);
  }

  /**
   * DELETE /api/suppliers/{id}
   * Deletes a supplier by ID.
   */
  deleteSupplier(id: number): Observable<void> {
    return this.api.delete<void>(`/suppliers/${id}`);
  }
}
