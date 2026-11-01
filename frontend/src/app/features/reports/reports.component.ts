import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReportService } from '../../core/services/report.service';
import { LocationService } from '../../core/services/location.service';
import { ProductService } from '../../core/services/product.service';
import { SupplierService } from '../../core/services/supplier.service';
import {
  ReportCategory,
  DateFilterPreset,
  InventoryOverviewReportResponse,
  StockStatusReportResponse,
  InventoryValuationReportResponse,
  StockMovementReportResponse,
  OrderSummaryReportResponse,
  PurchaseReceivingReportResponse,
  StockTransferReportResponse,
  Location,
  Category,
  Product,
  Supplier
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
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IconComponent,
    BadgeComponent,
    KpiCardComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    CurrencyFormatPipe
  ],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly locationService = inject(LocationService);
  private readonly productService = inject(ProductService);
  private readonly supplierService = inject(SupplierService);

  // Active Category Tab
  readonly activeTab = signal<ReportCategory>('inventory-overview');

  // Filter State Signals
  readonly datePreset = signal<DateFilterPreset>('all');
  readonly customStartDate = signal<string>('');
  readonly customEndDate = signal<string>('');
  readonly selectedLocationId = signal<number | null>(null);
  readonly selectedCategoryId = signal<number | null>(null);
  readonly selectedProductId = signal<number | null>(null);
  readonly selectedSupplierId = signal<number | null>(null);
  readonly selectedStatus = signal<string>('ALL');
  readonly selectedMovementType = signal<string>('ALL');
  readonly movementReferenceSearch = signal<string>('');

  // Dropdown reference signals
  readonly locations = signal<Location[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly products = signal<Product[]>([]);
  readonly suppliers = signal<Supplier[]>([]);

  // State Signals
  readonly isLoading = signal<boolean>(true);
  readonly isExporting = signal<boolean>(false);
  readonly hasError = signal<boolean>(false);
  readonly errorMessage = signal<string>('Unable to generate the requested inventory report.');

  // Report Data Signals
  readonly inventoryOverview = signal<InventoryOverviewReportResponse | null>(null);
  readonly stockStatus = signal<StockStatusReportResponse | null>(null);
  readonly inventoryValuation = signal<InventoryValuationReportResponse | null>(null);
  readonly stockMovements = signal<StockMovementReportResponse | null>(null);
  readonly orderSummary = signal<OrderSummaryReportResponse | null>(null);
  readonly purchaseReceiving = signal<PurchaseReceivingReportResponse | null>(null);
  readonly stockTransfers = signal<StockTransferReportResponse | null>(null);

  // Computed: Available vs Reserved percentage for Overview
  readonly physicalStockBreakdown = computed(() => {
    const data = this.inventoryOverview();
    if (!data || data.totalUnitsOnHand <= 0) return { availablePct: 0, reservedPct: 0 };
    const avail = Math.max(0, data.totalAvailableUnits);
    const res = Math.max(0, data.totalReservedUnits);
    const total = data.totalUnitsOnHand;
    return {
      availablePct: Math.round((avail / total) * 100),
      reservedPct: Math.round((res / total) * 100)
    };
  });

  // Computed: Stock Health status breakdown for Stock Status
  readonly stockStatusBreakdown = computed(() => {
    const data = this.stockStatus();
    if (!data || data.totalItems <= 0) return { inStockPct: 0, lowStockPct: 0, oosPct: 0 };
    const total = data.totalItems;
    return {
      inStockPct: Math.round((data.inStockCount / total) * 100),
      lowStockPct: Math.round((data.lowStockCount / total) * 100),
      oosPct: Math.round((data.outOfStockCount / total) * 100)
    };
  });

  ngOnInit(): void {
    this.loadReferenceData();
    this.loadActiveReport();
  }

  loadReferenceData(): void {
    this.locationService.getLocations().subscribe({
      next: (locs) => this.locations.set(locs),
      error: (e) => console.error('Error fetching locations:', e)
    });

    this.productService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: (e) => console.error('Error fetching categories:', e)
    });

    this.productService.getProducts().subscribe({
      next: (prods) => this.products.set(prods),
      error: (e) => console.error('Error fetching products:', e)
    });

    this.supplierService.getSuppliers().subscribe({
      next: (sups) => this.suppliers.set(sups),
      error: (e) => console.error('Error fetching suppliers:', e)
    });
  }

  selectTab(tab: ReportCategory): void {
    if (this.activeTab() === tab) return;
    this.activeTab.set(tab);
    // Reset specific secondary filters if switching contexts
    this.selectedStatus.set('ALL');
    this.selectedMovementType.set('ALL');
    this.movementReferenceSearch.set('');
    this.loadActiveReport();
  }

  setDatePreset(preset: DateFilterPreset): void {
    this.datePreset.set(preset);
    if (preset !== 'custom') {
      this.customStartDate.set('');
      this.customEndDate.set('');
    }
    this.loadActiveReport();
  }

  onFilterChange(): void {
    this.loadActiveReport();
  }

  loadActiveReport(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    const { startIso, endIso } = this.resolveDateRange();
    const tab = this.activeTab();

    switch (tab) {
      case 'inventory-overview':
        this.reportService.getInventoryOverview().subscribe({
          next: (res) => {
            this.inventoryOverview.set(res);
            this.isLoading.set(false);
          },
          error: (err) => this.handleError(err)
        });
        break;

      case 'stock-status':
        this.reportService.getStockStatus({
          locationId: this.selectedLocationId(),
          categoryId: this.selectedCategoryId(),
          productId: this.selectedProductId(),
          status: this.selectedStatus()
        }).subscribe({
          next: (res) => {
            this.stockStatus.set(res);
            this.isLoading.set(false);
          },
          error: (err) => this.handleError(err)
        });
        break;

      case 'inventory-valuation':
        this.reportService.getInventoryValuation({
          locationId: this.selectedLocationId(),
          categoryId: this.selectedCategoryId()
        }).subscribe({
          next: (res) => {
            this.inventoryValuation.set(res);
            this.isLoading.set(false);
          },
          error: (err) => this.handleError(err)
        });
        break;

      case 'stock-movement':
        this.reportService.getStockMovements({
          startDate: startIso,
          endDate: endIso,
          productId: this.selectedProductId(),
          locationId: this.selectedLocationId(),
          movementType: this.selectedMovementType(),
          reference: this.movementReferenceSearch()
        }).subscribe({
          next: (res) => {
            this.stockMovements.set(res);
            this.isLoading.set(false);
          },
          error: (err) => this.handleError(err)
        });
        break;

      case 'order-summary':
        this.reportService.getOrderSummary({
          startDate: startIso,
          endDate: endIso,
          status: this.selectedStatus(),
          locationId: this.selectedLocationId()
        }).subscribe({
          next: (res) => {
            this.orderSummary.set(res);
            this.isLoading.set(false);
          },
          error: (err) => this.handleError(err)
        });
        break;

      case 'purchase-receiving':
        this.reportService.getPurchaseReceivings({
          startDate: startIso,
          endDate: endIso,
          supplierId: this.selectedSupplierId(),
          locationId: this.selectedLocationId(),
          status: this.selectedStatus()
        }).subscribe({
          next: (res) => {
            this.purchaseReceiving.set(res);
            this.isLoading.set(false);
          },
          error: (err) => this.handleError(err)
        });
        break;

      case 'stock-transfers':
        this.reportService.getStockTransfers({
          startDate: startIso,
          endDate: endIso,
          productId: this.selectedProductId(),
          sourceLocationId: this.selectedLocationId(),
          status: this.selectedStatus()
        }).subscribe({
          next: (res) => {
            this.stockTransfers.set(res);
            this.isLoading.set(false);
          },
          error: (err) => this.handleError(err)
        });
        break;
    }
  }

  exportCurrentReport(): void {
    this.isExporting.set(true);
    try {
      const tab = this.activeTab();
      switch (tab) {
        case 'inventory-overview': {
          const data = this.inventoryOverview();
          if (data) this.reportService.exportInventoryOverviewCsv(data);
          break;
        }
        case 'stock-status': {
          const data = this.stockStatus();
          if (data) this.reportService.exportStockStatusCsv(data);
          break;
        }
        case 'inventory-valuation': {
          const data = this.inventoryValuation();
          if (data) this.reportService.exportInventoryValuationCsv(data);
          break;
        }
        case 'stock-movement': {
          const data = this.stockMovements();
          if (data) this.reportService.exportStockMovementsCsv(data);
          break;
        }
        case 'order-summary': {
          const data = this.orderSummary();
          if (data) this.reportService.exportOrderSummaryCsv(data);
          break;
        }
        case 'purchase-receiving': {
          const data = this.purchaseReceiving();
          if (data) this.reportService.exportPurchaseReceivingsCsv(data);
          break;
        }
        case 'stock-transfers': {
          const data = this.stockTransfers();
          if (data) this.reportService.exportStockTransfersCsv(data);
          break;
        }
      }
    } finally {
      setTimeout(() => this.isExporting.set(false), 500);
    }
  }

  // Resolve start and end ISO strings based on preset
  private resolveDateRange(): { startIso: string | null; endIso: string | null } {
    const preset = this.datePreset();
    const now = new Date();

    if (preset === 'all') {
      return { startIso: null, endIso: null };
    }

    if (preset === 'today') {
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
      return { startIso: start.toISOString(), endIso: end.toISOString() };
    }

    if (preset === 'last7') {
      const start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      start.setHours(0, 0, 0, 0);
      return { startIso: start.toISOString(), endIso: now.toISOString() };
    }

    if (preset === 'last30') {
      const start = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      start.setHours(0, 0, 0, 0);
      return { startIso: start.toISOString(), endIso: now.toISOString() };
    }

    if (preset === 'thisMonth') {
      const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
      return { startIso: start.toISOString(), endIso: now.toISOString() };
    }

    if (preset === 'custom') {
      const s = this.customStartDate();
      const e = this.customEndDate();
      const startIso = s ? `${s}T00:00:00` : null;
      const endIso = e ? `${e}T23:59:59` : null;
      return { startIso, endIso };
    }

    return { startIso: null, endIso: null };
  }

  private handleError(err: any): void {
    console.error('Failed to load report:', err);
    this.hasError.set(true);
    this.errorMessage.set(
      err?.error?.message || 'Failed to retrieve report data from the backend service. Please check connectivity.'
    );
    this.isLoading.set(false);
  }

  // Status and badge helpers
  getStatusBadgeVariant(status: string): BadgeVariant {
    switch (status?.toUpperCase()) {
      case 'IN_STOCK':
      case 'COMPLETED':
      case 'RECEIVED':
      case 'OPTIMAL':
        return 'success';
      case 'LOW_STOCK':
      case 'PENDING':
        return 'warning';
      case 'OUT_OF_STOCK':
      case 'CANCELLED':
      case 'CRITICAL':
        return 'danger';
      case 'NOT_STOCKED':
      default:
        return 'neutral';
    }
  }

  getMovementBadgeVariant(type: string): BadgeVariant {
    switch (type?.toUpperCase()) {
      case 'RECEIPT':
        return 'success';
      case 'TRANSFER':
        return 'info';
      case 'SALE':
        return 'neutral';
      case 'ADJUSTMENT':
        return 'warning';
      default:
        return 'neutral';
    }
  }

  isHistoricalTab(): boolean {
    const tab = this.activeTab();
    return tab === 'stock-movement' || tab === 'order-summary' || tab === 'purchase-receiving' || tab === 'stock-transfers';
  }
}
