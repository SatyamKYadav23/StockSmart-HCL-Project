import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StockMovementService } from '../../core/services/stock-movement.service';
import { StockMovementResponse, MovementType } from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  BadgeVariant,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent
} from '../../shared';

@Component({
  selector: 'app-movements',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IconComponent,
    BadgeComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent
  ],
  template: `
    <div class="feature-page-container">
      <div class="page-header">
        <div class="header-titles">
          <h2 class="page-title">Stock Movements Audit Log</h2>
          <p class="page-subtitle">Immutable ledger of all inventory adjustments, receipts, transfers, and dispatches</p>
        </div>
        <div class="header-actions">
          <button
            type="button"
            class="ss-btn ss-btn-secondary"
            (click)="loadMovements()"
            [disabled]="isLoading()"
            title="Refresh movements ledger"
          >
            <app-icon name="refresh" [size]="14"></app-icon>
            <span>Refresh</span>
          </button>
        </div>
      </div>

      <div class="ss-card">
        <div class="filter-bar">
          <div class="search-box">
            <app-icon name="search" [size]="14" extraClass="search-icon"></app-icon>
            <input
              type="text"
              class="ss-input ss-input-with-icon"
              placeholder="Search by ref #, product, SKU, facility, or notes..."
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event)"
            />
          </div>

          <div class="filter-controls">
            <select
              class="ss-select"
              [ngModel]="typeFilter()"
              (ngModelChange)="typeFilter.set($event)"
            >
              <option value="ALL">All Activity Types</option>
              <option value="ADJUSTMENT">Adjustments</option>
              <option value="STOCK_IN">Stock In (Receipts)</option>
              <option value="STOCK_OUT">Stock Out (Dispatches)</option>
              <option value="PURCHASE">Purchases</option>
              <option value="TRANSFER">Transfers</option>
              <option value="RETURN">Returns</option>
            </select>
          </div>
        </div>

        <div class="ss-table-container">
          <!-- Loading State -->
          <div *ngIf="isLoading()" style="padding: 16px;">
            <app-loading-skeleton *ngFor="let i of [1, 2, 3, 4, 5]" height="38px" extraClass="table-skeleton-row"></app-loading-skeleton>
          </div>

          <!-- Error State -->
          <div *ngIf="!isLoading() && hasError()" style="padding: 32px;">
            <app-error-state
              title="Unable to load stock movements"
              [description]="errorMessage()"
              [showRetry]="true"
              (retry)="loadMovements()"
            ></app-error-state>
          </div>

          <!-- Real Movements Table -->
          <table *ngIf="!isLoading() && !hasError() && filteredMovements().length > 0" class="ss-table">
            <thead>
              <tr>
                <th>Ref / Doc #</th>
                <th>Timestamp</th>
                <th>Activity Type</th>
                <th>Product & SKU</th>
                <th class="ss-table-numeric">Qty</th>
                <th>Facility Location</th>
                <th>Notes / Audit Details</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let m of filteredMovements()">
                <td class="font-mono font-semibold text-primary">
                  {{ m.referenceType || ('MOV-' + m.id) }}
                </td>
                <td>
                  <span class="text-xs text-muted font-mono">{{ m.createdAt | date:'short' }}</span>
                </td>
                <td>
                  <app-badge [variant]="getTypeVariant(m.movementType)" size="sm">
                    {{ m.movementType }}
                  </app-badge>
                </td>
                <td>
                  <div class="product-cell">
                    <span class="product-name font-semibold">{{ m.productName }}</span>
                    <span class="product-sku font-mono">{{ m.productSku || '—' }}</span>
                  </div>
                </td>
                <td class="ss-table-numeric">
                  <span
                    class="font-mono font-semibold"
                    [class.text-success]="isPositiveDelta(m)"
                    [class.text-danger]="isNegativeDelta(m)"
                  >
                    {{ getQuantityPrefix(m) }}{{ m.quantity }}
                  </span>
                </td>
                <td>
                  <span class="text-sm text-secondary">{{ m.locationName }}</span>
                </td>
                <td>
                  <span class="text-xs text-muted">{{ m.remarks || '—' }}</span>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Empty State -->
          <div *ngIf="!isLoading() && !hasError() && filteredMovements().length === 0" style="padding: 32px;">
            <app-empty-state
              icon="movements"
              title="No movement logs found"
              description="No ledger audit entries match your current search and type filters."
            ></app-empty-state>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .feature-page-container {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      max-width: 1600px;
      margin: 0 auto;
    }
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
    }
    .page-subtitle {
      font-size: 0.8125rem;
      color: var(--text-muted);
      margin-top: 2px;
    }
    .header-actions {
      display: flex;
      gap: 8px;
    }
    .filter-bar {
      padding: 14px 20px;
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      background-color: var(--color-slate-50);
      flex-wrap: wrap;
    }
    .search-box {
      flex: 1;
      max-width: 380px;
      position: relative;
    }
    .search-icon {
      position: absolute;
      left: 11px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--color-slate-400);
      pointer-events: none;
    }
    .product-cell {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .product-name {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-primary);
    }
    .product-sku {
      font-size: 0.6875rem;
      color: var(--text-muted);
    }
    .text-success { color: var(--color-success-solid, #10b981); }
    .text-danger { color: var(--color-danger-solid, #ef4444); }
  `]
})
export class MovementsComponent implements OnInit {
  private readonly movementService = inject(StockMovementService);

  readonly rawMovements = signal<StockMovementResponse[]>([]);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to retrieve stock movement logs from backend.');

  readonly searchQuery = signal('');
  readonly typeFilter = signal('ALL');

  readonly filteredMovements = computed<StockMovementResponse[]>(() => {
    const list = this.rawMovements();
    const q = this.searchQuery().trim().toLowerCase();
    const type = this.typeFilter();

    return list.filter(m => {
      const ref = (m.referenceType || '').toLowerCase();
      const pName = (m.productName || '').toLowerCase();
      const pSku = (m.productSku || '').toLowerCase();
      const loc = (m.locationName || '').toLowerCase();
      const rem = (m.remarks || '').toLowerCase();

      const matchesQuery =
        !q ||
        ref.includes(q) ||
        pName.includes(q) ||
        pSku.includes(q) ||
        loc.includes(q) ||
        rem.includes(q);

      const matchesType = type === 'ALL' || m.movementType === type;
      return matchesQuery && matchesType;
    });
  });

  ngOnInit(): void {
    this.loadMovements();
  }

  loadMovements(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.movementService.getMovements().subscribe({
      next: (data) => {
        // Sort descending by createdAt (newest first)
        const sorted = [...data].sort((a, b) => {
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
        this.rawMovements.set(sorted);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.errorMessage.set(err.error?.message || err.message || 'Failed to connect to stock movement ledger.');
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  getTypeVariant(type: string): BadgeVariant {
    switch (type) {
      case 'STOCK_IN':
      case 'PURCHASE':
        return 'success';
      case 'TRANSFER':
        return 'info';
      case 'STOCK_OUT':
      case 'SALE':
        return 'neutral';
      case 'ADJUSTMENT':
        return 'warning';
      case 'RETURN':
        return 'primary';
      default:
        return 'neutral';
    }
  }

  isPositiveDelta(m: StockMovementResponse): boolean {
    if (m.movementType === 'STOCK_IN' || m.movementType === 'PURCHASE' || m.movementType === 'RECEIPT') return true;
    if (m.movementType === 'TRANSFER') {
      const rem = (m.remarks || '').toLowerCase();
      return rem.includes('transfer in') || rem.includes('in from');
    }
    if (m.movementType === 'ADJUSTMENT') {
      const rem = (m.remarks || '').toLowerCase();
      return rem.includes('increase') || rem.includes('(+');
    }
    return false;
  }

  isNegativeDelta(m: StockMovementResponse): boolean {
    if (m.movementType === 'STOCK_OUT' || m.movementType === 'SALE' || m.movementType === 'ORDER') return true;
    if (m.movementType === 'TRANSFER') {
      const rem = (m.remarks || '').toLowerCase();
      return rem.includes('transfer out') || rem.includes('out to');
    }
    if (m.movementType === 'ADJUSTMENT') {
      const rem = (m.remarks || '').toLowerCase();
      return rem.includes('decrease') || rem.includes('(-');
    }
    return false;
  }

  getQuantityPrefix(m: StockMovementResponse): string {
    if (this.isPositiveDelta(m)) return '+';
    if (this.isNegativeDelta(m)) return '-';
    if (m.movementType === 'TRANSFER') return '→';
    return '';
  }
}
