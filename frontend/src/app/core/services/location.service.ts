import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Location, LocationRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class LocationService {
  private readonly api = inject(ApiService);

  /**
   * GET /api/locations
   * Fetches real facilities from Spring Boot backend.
   */
  getLocations(): Observable<Location[]> {
    return this.api.get<Location[]>('/locations');
  }

  /**
   * GET /api/locations/{id}
   */
  getLocationById(id: number): Observable<Location> {
    return this.api.get<Location>(`/locations/${id}`);
  }

  /**
   * POST /api/locations
   */
  createLocation(request: LocationRequest): Observable<Location> {
    return this.api.post<Location>('/locations', request);
  }

  /**
   * PUT /api/locations/{id}
   */
  updateLocation(id: number, request: LocationRequest): Observable<Location> {
    return this.api.put<Location>(`/locations/${id}`, request);
  }

  /**
   * DELETE /api/locations/{id}
   */
  deleteLocation(id: number): Observable<void> {
    return this.api.delete<void>(`/locations/${id}`);
  }
}
