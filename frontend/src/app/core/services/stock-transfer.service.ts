import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { StockTransferResponse, StockTransferRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class StockTransferService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/stock-transfers
   * Retrieves all inter-facility stock transfers.
   */
  getTransfers(): Observable<StockTransferResponse[]> {
    return this.api.get<StockTransferResponse[]>('/stock-transfers');
  }

  /**
   * GET /api/stock-transfers/{id}
   */
  getTransferById(id: number): Observable<StockTransferResponse> {
    return this.api.get<StockTransferResponse>(`/stock-transfers/${id}`);
  }

  /**
   * POST /api/stock-transfers
   * Atomically executes stock transfer between source and destination facilities.
   */
  createTransfer(request: StockTransferRequest): Observable<StockTransferResponse> {
    return this.api.post<StockTransferResponse>('/stock-transfers', request);
  }

  /**
   * DELETE /api/stock-transfers/{id}
   */
  deleteTransfer(id: number): Observable<void> {
    return this.api.delete<void>(`/stock-transfers/${id}`);
  }
}
