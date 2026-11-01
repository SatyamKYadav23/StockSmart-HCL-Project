import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StockTransferService } from '../../core/services/stock-transfer.service';
import { ProductService } from '../../core/services/product.service';
import { LocationService } from '../../core/services/location.service';
import { InventoryService } from '../../core/services/inventory.service';
import {
  StockTransferResponse,
  StockTransferRequest,
  Product,
  Location,
  InventoryResponse
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
  selector: 'app-stock-transfers',
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
  templateUrl: './stock-transfers.component.html',
  styleUrl: './stock-transfers.component.css'
})
export class StockTransfersComponent implements OnInit {
  private readonly transferService = inject(StockTransferService);
  private readonly productService = inject(ProductService);
  private readonly locationService = inject(LocationService);
  private readonly inventoryService = inject(InventoryService);
  private readonly fb = inject(FormBuilder);

  // Raw data signals
  readonly rawTransfers = signal<StockTransferResponse[]>([]);
  readonly products = signal<Product[]>([]);
  readonly locations = signal<Location[]>([]);
  readonly inventory = signal<InventoryResponse[]>([]);

  // State signals
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to retrieve stock transfers from backend.');

  // Filters
  readonly searchQuery = signal('');
  readonly statusFilter = signal('ALL');
  readonly sourceFilter = signal('ALL');
  readonly destinationFilter = signal('ALL');

  // Modal signals
  readonly selectedTransfer = signal<StockTransferResponse | null>(null);
  readonly isDetailsModalOpen = signal(false);
  readonly isCreateModalOpen = signal(false);
  readonly isSubmitting = signal(false);
  readonly modalError = signal<string | null>(null);
  readonly toastMessage = signal<string | null>(null);

  // Reactive Form
  readonly transferForm: FormGroup = this.fb.group({
    transferNumber: ['', [Validators.required, Validators.maxLength(50)]],
    transferDate: [this.getTodayIsoString(), [Validators.required]],
    productId: [null, [Validators.required]],
    sourceLocationId: [null, [Validators.required]],
    destinationLocationId: [null, [Validators.required]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    status: ['COMPLETED', [Validators.required]],
    remarks: ['', [Validators.maxLength(255)]]
  });

  // Filtered Transfers computed
  readonly filteredTransfers = computed<StockTransferResponse[]>(() => {
    const list = this.rawTransfers();
    const q = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();
    const src = this.sourceFilter();
    const dest = this.destinationFilter();

    return list.filter((trf) => {
      const num = (trf.transferNumber || '').toLowerCase();
      const prod = (trf.productName || '').toLowerCase();
      const sku = (trf.productSku || '').toLowerCase();
      const srcName = (trf.sourceLocationName || '').toLowerCase();
      const destName = (trf.destinationLocationName || '').toLowerCase();
      const remarks = (trf.remarks || '').toLowerCase();

      const matchesQuery =
        !q ||
        num.includes(q) ||
        prod.includes(q) ||
        sku.includes(q) ||
        srcName.includes(q) ||
        destName.includes(q) ||
        remarks.includes(q);

      const matchesStatus = status === 'ALL' || (trf.status || '').toUpperCase() === status.toUpperCase();
      const matchesSource = src === 'ALL' || String(trf.sourceLocationId) === src;
      const matchesDest = dest === 'ALL' || String(trf.destinationLocationId) === dest;

      return matchesQuery && matchesStatus && matchesSource && matchesDest;
    });
  });

  // KPIs computed
  readonly totalTransfersCount = computed(() => this.rawTransfers().length);

  readonly totalUnitsTransferred = computed(() =>
    this.rawTransfers().reduce((sum, t) => sum + (t.quantity || 0), 0)
  );

  readonly activeFacilitiesCount = computed(() => {
    const locSet = new Set<number>();
    this.rawTransfers().forEach((t) => {
      if (t.sourceLocationId) locSet.add(t.sourceLocationId);
      if (t.destinationLocationId) locSet.add(t.destinationLocationId);
    });
    return locSet.size;
  });

  readonly completedTransfersCount = computed(() =>
    this.rawTransfers().filter((t) => (t.status || '').toUpperCase() === 'COMPLETED').length
  );

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.transferService.getTransfers().subscribe({
      next: (data) => {
        this.rawTransfers.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load stock transfers:', err);
        this.hasError.set(true);
        this.errorMessage.set(err?.message || 'Failed to connect to backend server.');
        this.isLoading.set(false);
      }
    });

    this.productService.getProducts().subscribe({
      next: (prods) => this.products.set(prods || []),
      error: (err) => console.error('Failed to load products for transfer:', err)
    });

    this.locationService.getLocations().subscribe({
      next: (locs) => this.locations.set(locs || []),
      error: (err) => console.error('Failed to load locations for transfer:', err)
    });

    this.inventoryService.getInventory().subscribe({
      next: (inv) => this.inventory.set(inv || []),
      error: (err) => console.error('Failed to load inventory for transfer:', err)
    });
  }

  // Stock inspection helpers
  getSourceInventory(): { quantity: number; reservedQuantity: number; available: number } {
    const prodId = Number(this.transferForm.get('productId')?.value);
    const srcId = Number(this.transferForm.get('sourceLocationId')?.value);
    if (!prodId || !srcId) {
      return { quantity: 0, reservedQuantity: 0, available: 0 };
    }
    const inv = this.inventory().find((i) => i.productId === prodId && i.locationId === srcId);
    if (!inv) {
      return { quantity: 0, reservedQuantity: 0, available: 0 };
    }
    const available = Math.max(0, inv.quantity - (inv.reservedQuantity || 0));
    return {
      quantity: inv.quantity,
      reservedQuantity: inv.reservedQuantity || 0,
      available
    };
  }

  getDestinationInventory(): number {
    const prodId = Number(this.transferForm.get('productId')?.value);
    const destId = Number(this.transferForm.get('destinationLocationId')?.value);
    if (!prodId || !destId) {
      return 0;
    }
    const inv = this.inventory().find((i) => i.productId === prodId && i.locationId === destId);
    return inv ? inv.quantity : 0;
  }

  getLocationName(id: number | null): string {
    if (!id) return '—';
    const loc = this.locations().find((l) => l.id === Number(id));
    return loc ? loc.name : `Facility #${id}`;
  }

  getProductName(id: number | null): string {
    if (!id) return '—';
    const p = this.products().find((item) => item.id === Number(id));
    return p ? `${p.sku} — ${p.name}` : `Product #${id}`;
  }

  openCreateModal(): void {
    this.modalError.set(null);
    this.isSubmitting.set(false);

    const generatedNumber = `TRF-${Date.now().toString().slice(-6)}`;
    const prods = this.products();
    const locs = this.locations();

    this.transferForm.reset({
      transferNumber: generatedNumber,
      transferDate: this.getTodayIsoString(),
      productId: prods.length > 0 ? prods[0].id : null,
      sourceLocationId: locs.length > 0 ? locs[0].id : null,
      destinationLocationId: locs.length > 1 ? locs[1].id : null,
      quantity: 1,
      status: 'COMPLETED',
      remarks: ''
    });

    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    if (!this.isSubmitting()) {
      this.isCreateModalOpen.set(false);
    }
  }

  openDetailsModal(transfer: StockTransferResponse): void {
    this.selectedTransfer.set(transfer);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    this.selectedTransfer.set(null);
  }

  submitTransfer(): void {
    if (this.isSubmitting()) return;

    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      this.modalError.set('Please check the form fields and ensure all inputs are valid.');
      return;
    }

    const formVal = this.transferForm.value;
    const sourceId = Number(formVal.sourceLocationId);
    const destId = Number(formVal.destinationLocationId);
    const qty = Number(formVal.quantity);

    if (sourceId === destId) {
      this.modalError.set('Source and destination facilities must be different.');
      return;
    }

    if (qty <= 0) {
      this.modalError.set('Transfer quantity must be greater than zero.');
      return;
    }

    const srcInv = this.getSourceInventory();
    if (qty > srcInv.available) {
      this.modalError.set(
        `Insufficient available stock at ${this.getLocationName(sourceId)}. Available: ${srcInv.available}, Requested: ${qty}`
      );
      return;
    }

    this.isSubmitting.set(true);
    this.modalError.set(null);

    const request: StockTransferRequest = {
      transferNumber: formVal.transferNumber.trim(),
      transferDate: formVal.transferDate ? new Date(formVal.transferDate).toISOString() : new Date().toISOString(),
      productId: Number(formVal.productId),
      sourceLocationId: sourceId,
      destinationLocationId: destId,
      quantity: qty,
      status: formVal.status,
      remarks: formVal.remarks?.trim() || undefined
    };

    this.transferService.createTransfer(request).subscribe({
      next: (created) => {
        this.isSubmitting.set(false);
        this.isCreateModalOpen.set(false);
        this.showToast(`Transfer ${created.transferNumber} executed successfully! Inventory transferred.`);
        this.loadAllData();
      },
      error: (err) => {
        console.error('Failed to create stock transfer:', err);
        this.isSubmitting.set(false);
        if (err.status === 409) {
          this.modalError.set(err?.error?.message || `Transfer #${formVal.transferNumber} already exists.`);
        } else {
          this.modalError.set(err?.error?.message || err?.message || 'Failed to execute stock transfer.');
        }
      }
    });
  }

  deleteTransfer(transfer: StockTransferResponse, event: MouseEvent): void {
    event.stopPropagation();
    if ((transfer.status || '').toUpperCase() === 'COMPLETED') {
      alert('Completed stock transfers are immutable and cannot be deleted because they affect inventory history.');
      return;
    }

    if (!confirm(`Are you sure you want to delete transfer ${transfer.transferNumber}?`)) {
      return;
    }

    this.transferService.deleteTransfer(transfer.id).subscribe({
      next: () => {
        this.showToast(`Transfer ${transfer.transferNumber} deleted.`);
        this.loadAllData();
      },
      error: (err) => {
        console.error('Failed to delete transfer:', err);
        alert(err?.error?.message || 'Failed to delete transfer.');
      }
    });
  }

  getStatusVariant(status: string): BadgeVariant {
    switch ((status || '').toUpperCase()) {
      case 'COMPLETED':
        return 'success';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  showToast(msg: string): void {
    this.toastMessage.set(msg);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 4500);
  }

  private getTodayIsoString(): string {
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
  }
}
