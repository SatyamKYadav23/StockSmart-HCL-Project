import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { InventoryService } from '../../core/services/inventory.service';
import { ProductService } from '../../core/services/product.service';
import { LocationService } from '../../core/services/location.service';
import {
  InventoryResponse,
  StockAdjustmentRequest,
  InventoryItem,
  StockStatus,
  Product,
  Location
} from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  BadgeVariant,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent
} from '../../shared';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    IconComponent,
    BadgeComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent
  ],
  templateUrl: './inventory.component.html',
  styleUrl: './inventory.component.css'
})
export class InventoryComponent implements OnInit {
  private readonly inventoryService = inject(InventoryService);
  private readonly productService = inject(ProductService);
  private readonly locationService = inject(LocationService);
  private readonly fb = inject(FormBuilder);

  // Raw data signals
  readonly rawInventory = signal<InventoryResponse[]>([]);
  readonly products = signal<Product[]>([]);
  readonly locations = signal<Location[]>([]);

  // State signals
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to retrieve inventory data from backend.');

  // Filter & Search signals
  readonly searchQuery = signal('');
  readonly statusFilter = signal<'ALL' | StockStatus>('ALL');
  readonly locationFilter = signal<'ALL' | number>('ALL');

  // Details Modal signals
  readonly selectedItem = signal<InventoryItem | null>(null);
  readonly isDetailsModalOpen = signal(false);

  // Stock Adjustment Modal signals
  readonly isAdjustmentModalOpen = signal(false);
  readonly isSubmittingAdjustment = signal(false);
  readonly adjustmentError = signal<string | null>(null);
  readonly toastMessage = signal<string | null>(null);

  // Reactive Form
  readonly adjustmentForm: FormGroup = this.fb.group({
    productId: [null, [Validators.required]],
    locationId: [null, [Validators.required]],
    adjustmentType: ['INCREASE', [Validators.required]], // 'INCREASE' | 'DECREASE'
    quantity: [null, [Validators.required, Validators.min(1)]],
    referenceNumber: [''],
    notes: ['']
  });

  // Track form values as signal for live stock calculation
  readonly formSelectedProductId = signal<number | null>(null);
  readonly formSelectedLocationId = signal<number | null>(null);
  readonly formAdjustmentType = signal<'INCREASE' | 'DECREASE'>('INCREASE');
  readonly formAdjustmentQty = signal<number | null>(null);

  // Computed existing stock for currently selected product & location
  readonly matchedExistingRecord = computed<InventoryResponse | null>(() => {
    const pId = this.formSelectedProductId();
    const lId = this.formSelectedLocationId();
    if (!pId || !lId) return null;
    return this.rawInventory().find(i => i.productId === pId && i.locationId === lId) || null;
  });

  readonly currentStockPreview = computed<number>(() => {
    const record = this.matchedExistingRecord();
    return record ? record.quantity : 0;
  });

  readonly hasExistingRecord = computed<boolean>(() => {
    return this.matchedExistingRecord() !== null;
  });

  readonly newStockPreview = computed<number>(() => {
    const current = this.currentStockPreview();
    const qty = this.formAdjustmentQty() || 0;
    const type = this.formAdjustmentType();
    return type === 'INCREASE' ? current + qty : current - qty;
  });

  readonly isNegativeStockError = computed<boolean>(() => {
    const qty = this.formAdjustmentQty() || 0;
    const type = this.formAdjustmentType();
    const current = this.currentStockPreview();
    if (type === 'DECREASE') {
      if (!this.hasExistingRecord()) return true;
      return (current - qty < 0);
    }
    return false;
  });

  // Enriched items signal: merges inventory with product SKU and reorder levels
  readonly enrichedItems = computed<InventoryItem[]>(() => {
    const raw = this.rawInventory();
    const prodList = this.products();
    const productMap = new Map<number, Product>(prodList.map(p => [p.id, p]));

    return raw.map((item) => {
      const prod = productMap.get(item.productId);
      const reorderLvl = prod ? prod.reorderLevel : 0;
      const sku = prod ? prod.sku : '—';
      const available = Math.max(0, (item.quantity || 0) - (item.reservedQuantity || 0));

      let status: StockStatus = 'IN_STOCK';
      if ((item.quantity || 0) <= 0) {
        status = 'OUT_OF_STOCK';
      } else if ((item.quantity || 0) <= reorderLvl) {
        status = 'LOW_STOCK';
      }

      return {
        ...item,
        sku,
        reorderLevel: reorderLvl,
        availableQuantity: available,
        status
      };
    });
  });

  // Filtered Inventory items
  readonly filteredItems = computed<InventoryItem[]>(() => {
    const list = this.enrichedItems();
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();
    const locId = this.locationFilter();

    return list.filter((item) => {
      const matchesSearch =
        !query ||
        item.productName.toLowerCase().includes(query) ||
        (item.sku && item.sku.toLowerCase().includes(query)) ||
        item.locationName.toLowerCase().includes(query);

      const matchesStatus = status === 'ALL' || item.status === status;
      const matchesLocation = locId === 'ALL' || item.locationId === Number(locId);

      return matchesSearch && matchesStatus && matchesLocation;
    });
  });

  ngOnInit(): void {
    this.loadAllData();

    // Listen to form value changes to drive reactive preview
    this.adjustmentForm.get('productId')?.valueChanges.subscribe(val => {
      this.formSelectedProductId.set(val ? Number(val) : null);
    });
    this.adjustmentForm.get('locationId')?.valueChanges.subscribe(val => {
      this.formSelectedLocationId.set(val ? Number(val) : null);
    });
    this.adjustmentForm.get('adjustmentType')?.valueChanges.subscribe(val => {
      this.formAdjustmentType.set(val || 'INCREASE');
    });
    this.adjustmentForm.get('quantity')?.valueChanges.subscribe(val => {
      this.formAdjustmentQty.set(val ? Number(val) : null);
    });
  }

  loadAllData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.inventoryService.getInventory().subscribe({
      next: (inv) => {
        this.rawInventory.set(inv);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.errorMessage.set(err.error?.message || err.message || 'Failed to load inventory from server.');
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });

    this.productService.getProducts().subscribe({
      next: (prods) => this.products.set(prods),
      error: () => {}
    });

    this.locationService.getLocations().subscribe({
      next: (locs) => this.locations.set(locs),
      error: () => {}
    });
  }

  // --- Details Modal Actions ---

  openDetailsModal(item: InventoryItem): void {
    this.selectedItem.set(item);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    this.selectedItem.set(null);
  }

  // --- Adjustment Modal Action ---

  openAdjustmentModal(item?: InventoryItem): void {
    this.adjustmentError.set(null);
    this.isSubmittingAdjustment.set(false);

    if (item) {
      this.adjustmentForm.reset({
        productId: item.productId,
        locationId: item.locationId,
        adjustmentType: 'INCREASE',
        quantity: null,
        referenceNumber: `ADJ-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`,
        notes: ''
      });
      this.formSelectedProductId.set(item.productId);
      this.formSelectedLocationId.set(item.locationId);
    } else {
      this.adjustmentForm.reset({
        productId: this.products().length > 0 ? this.products()[0].id : null,
        locationId: this.locations().length > 0 ? this.locations()[0].id : null,
        adjustmentType: 'INCREASE',
        quantity: null,
        referenceNumber: `ADJ-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`,
        notes: ''
      });
      this.formSelectedProductId.set(this.products().length > 0 ? this.products()[0].id : null);
      this.formSelectedLocationId.set(this.locations().length > 0 ? this.locations()[0].id : null);
    }

    this.formAdjustmentType.set('INCREASE');
    this.formAdjustmentQty.set(null);
    this.isAdjustmentModalOpen.set(true);
  }

  closeAdjustmentModal(): void {
    this.isAdjustmentModalOpen.set(false);
    this.adjustmentError.set(null);
    this.isSubmittingAdjustment.set(false);
  }

  setAdjustmentType(type: 'INCREASE' | 'DECREASE'): void {
    this.adjustmentForm.patchValue({ adjustmentType: type });
  }

  submitAdjustment(): void {
    if (this.isSubmittingAdjustment()) return;

    if (this.adjustmentForm.invalid || this.isNegativeStockError() || (!this.hasExistingRecord() && this.formAdjustmentType() === 'DECREASE')) {
      this.adjustmentForm.markAllAsTouched();
      return;
    }

    this.isSubmittingAdjustment.set(true);
    this.adjustmentError.set(null);

    const fv = this.adjustmentForm.value;
    const qty = Number(fv.quantity);
    const delta = fv.adjustmentType === 'INCREASE' ? qty : -qty;

    const request: StockAdjustmentRequest = {
      productId: Number(fv.productId),
      locationId: Number(fv.locationId),
      quantityChange: delta,
      referenceNumber: fv.referenceNumber?.trim() || undefined,
      notes: fv.notes?.trim() || undefined
    };

    this.inventoryService.adjustStock(request).subscribe({
      next: (updatedInventory) => {
        this.isSubmittingAdjustment.set(false);
        this.closeAdjustmentModal();

        const changeStr = delta > 0 ? `+${delta}` : `${delta}`;
        this.showToast(`Stock adjustment recorded successfully (${changeStr} units for ${updatedInventory.productName}).`);
        this.loadAllData();
      },
      error: (err: any) => {
        this.isSubmittingAdjustment.set(false);
        const msg = err.error?.message || err.message || 'Failed to submit stock adjustment.';
        this.adjustmentError.set(msg);
      }
    });
  }

  showToast(message: string): void {
    this.toastMessage.set(message);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 4500);
  }

  getStatusVariant(status: StockStatus): BadgeVariant {
    switch (status) {
      case 'IN_STOCK':
        return 'success';
      case 'LOW_STOCK':
        return 'warning';
      case 'OUT_OF_STOCK':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  onLocationFilterChange(val: string): void {
    if (val === 'ALL') {
      this.locationFilter.set('ALL');
    } else {
      this.locationFilter.set(Number(val));
    }
  }
}
