import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PurchaseReceivingResponse, PurchaseReceivingRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class PurchaseReceivingService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/purchase-receivings
   * Retrieves all purchase receiving transactions from the Spring Boot backend.
   */
  getReceivings(): Observable<PurchaseReceivingResponse[]> {
    return this.api.get<PurchaseReceivingResponse[]>('/purchase-receivings');
  }

  /**
   * GET /api/purchase-receivings/{id}
   */
  getReceivingById(id: number): Observable<PurchaseReceivingResponse> {
    return this.api.get<PurchaseReceivingResponse>(`/purchase-receivings/${id}`);
  }

  /**
   * POST /api/purchase-receivings
   * Records stock received from a supplier, updating inventory and movement audit atomically.
   */
  createReceiving(request: PurchaseReceivingRequest): Observable<PurchaseReceivingResponse> {
    return this.api.post<PurchaseReceivingResponse>('/purchase-receivings', request);
  }

  /**
   * DELETE /api/purchase-receivings/{id}
   */
  deleteReceiving(id: number): Observable<void> {
    return this.api.delete<void>(`/purchase-receivings/${id}`);
  }
}
