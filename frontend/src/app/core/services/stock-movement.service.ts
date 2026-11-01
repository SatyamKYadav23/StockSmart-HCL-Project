import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { StockMovementResponse } from '../models';

@Injectable({
  providedIn: 'root'
})
export class StockMovementService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/stock-movements
   * Fetches real stock movement audit logs from Spring Boot backend.
   */
  getMovements(): Observable<StockMovementResponse[]> {
    return this.api.get<StockMovementResponse[]>('/stock-movements');
  }

  /**
   * GET /api/stock-movements/{id}
   */
  getMovementById(id: number): Observable<StockMovementResponse> {
    return this.api.get<StockMovementResponse>(`/stock-movements/${id}`);
  }
}
