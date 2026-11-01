import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { OrderResponse, OrderRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/orders
   * Retrieves all orders from the Spring Boot backend.
   */
  getOrders(): Observable<OrderResponse[]> {
    return this.api.get<OrderResponse[]>('/orders');
  }

  /**
   * GET /api/orders/{id}
   */
  getOrderById(id: number): Observable<OrderResponse> {
    return this.api.get<OrderResponse>(`/orders/${id}`);
  }

  /**
   * POST /api/orders
   * Creates an order with items, updating inventory and movement audit if COMPLETED.
   */
  createOrder(request: OrderRequest): Observable<OrderResponse> {
    return this.api.post<OrderResponse>('/orders', request);
  }

  /**
   * PUT /api/orders/{id}
   */
  updateOrder(id: number, request: OrderRequest): Observable<OrderResponse> {
    return this.api.put<OrderResponse>(`/orders/${id}`, request);
  }

  /**
   * DELETE /api/orders/{id}
   */
  deleteOrder(id: number): Observable<void> {
    return this.api.delete<void>(`/orders/${id}`);
  }
}
