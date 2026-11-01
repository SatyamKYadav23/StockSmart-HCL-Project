import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DashboardService } from '../../core/services/dashboard.service';
import {
  DashboardSummary,
  StockAlert,
  StockMovement,
  LocationInventorySummary,
  OrderResponse
} from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  BadgeVariant,
  KpiCardComponent,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent,
  CurrencyFormatPipe
} from '../../shared';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    IconComponent,
    BadgeComponent,
    KpiCardComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    CurrencyFormatPipe
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Could not load inventory dashboard metrics. Please verify system connectivity.');
  readonly data = signal<DashboardSummary | null>(null);
  readonly isExporting = signal(false);

  // Filter & Search states for the operational tables
  readonly alertSearch = signal('');
  readonly alertStatusFilter = signal<'ALL' | 'LOW_STOCK' | 'CRITICAL' | 'OUT_OF_STOCK'>('ALL');
  readonly movementTypeFilter = signal<string>('ALL');
  readonly orderStatusFilter = signal<string>('ALL');

  // Filtered Stock Alerts
  readonly filteredAlerts = computed(() => {
    const summary = this.data();
    if (!summary?.stockAlerts) return [];

    const search = this.alertSearch().trim().toLowerCase();
    const filter = this.alertStatusFilter();

    return summary.stockAlerts.filter(alert => {
      const matchesSearch =
        !search ||
        alert.productName.toLowerCase().includes(search) ||
        alert.sku.toLowerCase().includes(search) ||
        alert.locationName.toLowerCase().includes(search);

      const matchesStatus = filter === 'ALL' || alert.status === filter;

      return matchesSearch && matchesStatus;
    });
  });

  // Filtered Recent Movements
  readonly filteredMovements = computed(() => {
    const summary = this.data();
    if (!summary?.recentMovements) return [];

    const filter = this.movementTypeFilter();
    if (filter === 'ALL') return summary.recentMovements;

    return summary.recentMovements.filter(m => m.type.toUpperCase() === filter.toUpperCase());
  });

  // Filtered Recent Orders
  readonly filteredOrders = computed(() => {
    const summary = this.data();
    if (!summary?.recentOrders) return [];

    const filter = this.orderStatusFilter();
    if (filter === 'ALL') return summary.recentOrders;

    return summary.recentOrders.filter(o => o.status.toUpperCase() === filter.toUpperCase());
  });

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.dashboardService.getSummary().subscribe({
      next: (summary) => {
        this.data.set(summary);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load dashboard data:', err);
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  exportSummary(): void {
    const summary = this.data();
    if (!summary) return;

    this.isExporting.set(true);
    try {
      this.dashboardService.exportSummaryCsv(summary);
    } finally {
      setTimeout(() => this.isExporting.set(false), 600);
    }
  }

  getAlertBadgeVariant(status: StockAlert['status']): BadgeVariant {
    switch (status) {
      case 'CRITICAL':
      case 'OUT_OF_STOCK':
        return 'danger';
      case 'LOW_STOCK':
        return 'warning';
      default:
        return 'neutral';
    }
  }

  getAlertStatusLabel(status: StockAlert['status']): string {
    switch (status) {
      case 'OUT_OF_STOCK':
        return 'Out of Stock';
      case 'CRITICAL':
        return 'Critical Stock';
      case 'LOW_STOCK':
        return 'Low Stock';
      default:
        return status;
    }
  }

  getMovementBadgeVariant(type: string): BadgeVariant {
    switch (type.toUpperCase()) {
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
        return 'info';
      default:
        return 'neutral';
    }
  }

  getMovementQuantityPrefix(type: string): string {
    switch (type.toUpperCase()) {
      case 'STOCK_IN':
      case 'PURCHASE':
      case 'RETURN':
        return '+';
      case 'TRANSFER':
        return '→';
      case 'STOCK_OUT':
      case 'SALE':
        return '-';
      default:
        return '';
    }
  }

  getLocationStatusVariant(status: string): BadgeVariant {
    switch (status) {
      case 'OPTIMAL':
      case 'ACTIVE':
        return 'success';
      case 'LOW_STOCK':
        return 'warning';
      case 'CRITICAL':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  getOrderStatusVariant(status: string): BadgeVariant {
    switch (status.toUpperCase()) {
      case 'COMPLETED':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'neutral';
    }
  }
}
