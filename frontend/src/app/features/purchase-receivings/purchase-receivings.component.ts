import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { PurchaseReceivingService } from '../../core/services/purchase-receiving.service';
import { ProductService } from '../../core/services/product.service';
import { LocationService } from '../../core/services/location.service';
import { SupplierService } from '../../core/services/supplier.service';
import { InventoryService } from '../../core/services/inventory.service';
import {
  PurchaseReceivingResponse,
  PurchaseReceivingItemResponse,
  PurchaseReceivingRequest,
  PurchaseReceivingItemRequest,
  Product,
  Location,
  Supplier,
  InventoryResponse
} from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  BadgeVariant,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent,
  CurrencyFormatPipe
} from '../../shared';

@Component({
  selector: 'app-purchase-receivings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    IconComponent,
    BadgeComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    CurrencyFormatPipe
  ],
  templateUrl: './purchase-receivings.component.html',
  styleUrl: './purchase-receivings.component.css'
})
export class PurchaseReceivingsComponent implements OnInit {
  private readonly receivingService = inject(PurchaseReceivingService);
  private readonly productService = inject(ProductService);
  private readonly locationService = inject(LocationService);
  private readonly supplierService = inject(SupplierService);
  private readonly inventoryService = inject(InventoryService);
  private readonly fb = inject(FormBuilder);

  // Raw data signals
  readonly rawReceivings = signal<PurchaseReceivingResponse[]>([]);
  readonly products = signal<Product[]>([]);
  readonly locations = signal<Location[]>([]);
  readonly suppliers = signal<Supplier[]>([]);
  readonly inventory = signal<InventoryResponse[]>([]);

  // State signals
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to retrieve purchase receivings from backend.');

  // Filter & Search signals
  readonly searchQuery = signal('');
  readonly statusFilter = signal<string>('ALL');

  // Details Modal signals
  readonly selectedReceiving = signal<PurchaseReceivingResponse | null>(null);
  readonly isDetailsModalOpen = signal(false);

  // Create Modal signals
  readonly isCreateModalOpen = signal(false);
  readonly isSubmitting = signal(false);
  readonly modalError = signal<string | null>(null);
  readonly toastMessage = signal<string | null>(null);

  // Create Purchase Receiving Form
  readonly receivingForm: FormGroup = this.fb.group({
    receivingNumber: ['', [Validators.required, Validators.maxLength(50)]],
    supplierId: [null, [Validators.required]],
    locationId: [null, [Validators.required]],
    status: ['RECEIVED', [Validators.required]],
    receivingDate: [this.getTodayIsoString(), [Validators.required]],
    remarks: ['', [Validators.maxLength(255)]],
    items: this.fb.array([])
  });

  get itemsArray(): FormArray {
    return this.receivingForm.get('items') as FormArray;
  }

  // Filtered Receivings
  readonly filteredReceivings = computed<PurchaseReceivingResponse[]>(() => {
    const list = this.rawReceivings();
    const q = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    return list.filter((rcv) => {
      const num = (rcv.receivingNumber || '').toLowerCase();
      const sup = (rcv.supplierName || '').toLowerCase();
      const loc = (rcv.locationName || '').toLowerCase();
      const rem = (rcv.remarks || '').toLowerCase();

      const matchesSearch = !q || num.includes(q) || sup.includes(q) || loc.includes(q) || rem.includes(q);
      const matchesStatus = status === 'ALL' || (rcv.status || '').toUpperCase() === status.toUpperCase();

      return matchesSearch && matchesStatus;
    });
  });

  // KPI Metrics Computed
  readonly totalReceivingsCount = computed(() => this.rawReceivings().length);

  readonly totalValueReceived = computed(() =>
    this.rawReceivings().reduce((sum, r) => sum + (Number(r.totalAmount) || 0), 0)
  );

  readonly totalUnitsReceived = computed(() =>
    this.rawReceivings().reduce(
      (sum, r) => sum + (r.items ? r.items.reduce((iSum, i) => iSum + (i.quantity || 0), 0) : 0),
      0
    )
  );

  readonly activeSuppliersCount = computed(() => {
    const uniqueIds = new Set(this.rawReceivings().map((r) => r.supplierId));
    return uniqueIds.size;
  });

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    // Fetch receivings, products, locations, suppliers, inventory concurrently
    this.receivingService.getReceivings().subscribe({
      next: (data) => {
        this.rawReceivings.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load purchase receivings:', err);
        this.hasError.set(true);
        this.errorMessage.set(err?.message || 'Failed to connect to backend server.');
        this.isLoading.set(false);
      }
    });

    this.productService.getProducts().subscribe({
      next: (prods) => this.products.set(prods || []),
      error: (err) => console.error('Failed to load products for receiving:', err)
    });

    this.locationService.getLocations().subscribe({
      next: (locs) => this.locations.set(locs || []),
      error: (err) => console.error('Failed to load locations for receiving:', err)
    });

    this.supplierService.getSuppliers().subscribe({
      next: (sups) => this.suppliers.set(sups || []),
      error: (err) => console.error('Failed to load suppliers for receiving:', err)
    });

    this.inventoryService.getInventory().subscribe({
      next: (inv) => this.inventory.set(inv || []),
      error: (err) => console.error('Failed to load inventory for receiving preview:', err)
    });
  }

  // Stock preview helpers
  getCurrentStock(productId: number | null, locationId: number | null): number {
    if (!productId || !locationId) return 0;
    const item = this.inventory().find(
      (inv) => inv.productId === Number(productId) && inv.locationId === Number(locationId)
    );
    return item ? item.quantity : 0;
  }

  getProjectedStock(productId: number | null, locationId: number | null, receivedQty: number | null): number {
    const current = this.getCurrentStock(productId, locationId);
    return current + (Number(receivedQty) || 0);
  }

  // Line item helpers
  addItem(productId: number | null = null, quantity: number = 1, unitCost: number = 0): void {
    const group = this.fb.group({
      productId: [productId, [Validators.required]],
      quantity: [quantity, [Validators.required, Validators.min(1)]],
      unitCost: [unitCost, [Validators.required, Validators.min(0)]]
    });
    this.itemsArray.push(group);
  }

  removeItem(index: number): void {
    if (this.itemsArray.length > 1) {
      this.itemsArray.removeAt(index);
    }
  }

  onProductSelect(index: number): void {
    const group = this.itemsArray.at(index) as FormGroup;
    const selectedProdId = group.get('productId')?.value;
    if (selectedProdId) {
      const prod = this.products().find((p) => p.id === Number(selectedProdId));
      if (prod) {
        // Auto-fill unitCost with product costPrice, or price if costPrice is undefined/zero
        const defaultCost = prod.costPrice && prod.costPrice > 0 ? prod.costPrice : (prod.price || 0);
        group.patchValue({ unitCost: defaultCost });
      }
    }
  }

  getItemSubtotal(index: number): number {
    const group = this.itemsArray.at(index);
    if (!group) return 0;
    const qty = Number(group.get('quantity')?.value) || 0;
    const cost = Number(group.get('unitCost')?.value) || 0;
    return qty * cost;
  }

  getFormGrandTotal(): number {
    let total = 0;
    for (let i = 0; i < this.itemsArray.length; i++) {
      total += this.getItemSubtotal(i);
    }
    return total;
  }

  getFormTotalUnits(): number {
    let units = 0;
    for (let i = 0; i < this.itemsArray.length; i++) {
      units += Number(this.itemsArray.at(i).get('quantity')?.value) || 0;
    }
    return units;
  }

  // Open modals
  openCreateModal(): void {
    this.modalError.set(null);
    this.isSubmitting.set(false);

    // Auto-generate receiving number
    const generatedNum = `RCV-${Date.now().toString().slice(-6)}`;

    // Reset form
    this.receivingForm.reset({
      receivingNumber: generatedNum,
      supplierId: this.suppliers().length > 0 ? this.suppliers()[0].id : null,
      locationId: this.locations().length > 0 ? this.locations()[0].id : null,
      status: 'RECEIVED',
      receivingDate: this.getTodayIsoString(),
      remarks: ''
    });

    // Clear and add 1 default line item
    while (this.itemsArray.length > 0) {
      this.itemsArray.removeAt(0);
    }

    const firstProduct = this.products().length > 0 ? this.products()[0] : null;
    const initialCost = firstProduct ? (firstProduct.costPrice || firstProduct.price || 0) : 0;
    this.addItem(firstProduct ? firstProduct.id : null, 1, initialCost);

    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    if (!this.isSubmitting()) {
      this.isCreateModalOpen.set(false);
    }
  }

  openDetailsModal(receiving: PurchaseReceivingResponse): void {
    this.selectedReceiving.set(receiving);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    this.selectedReceiving.set(null);
  }

  submitReceiving(): void {
    if (this.isSubmitting()) return;

    if (this.receivingForm.invalid) {
      this.receivingForm.markAllAsTouched();
      this.modalError.set('Please check the form fields and ensure all required inputs are valid.');
      return;
    }

    if (this.itemsArray.length === 0) {
      this.modalError.set('A purchase receipt must include at least one product item.');
      return;
    }

    this.isSubmitting.set(true);
    this.modalError.set(null);

    const formVal = this.receivingForm.value;
    const itemsReq: PurchaseReceivingItemRequest[] = formVal.items.map((i: any) => ({
      productId: Number(i.productId),
      quantity: Number(i.quantity),
      unitCost: Number(i.unitCost)
    }));

    const request: PurchaseReceivingRequest = {
      receivingNumber: formVal.receivingNumber.trim(),
      supplierId: Number(formVal.supplierId),
      locationId: Number(formVal.locationId),
      status: formVal.status,
      receivingDate: formVal.receivingDate ? new Date(formVal.receivingDate).toISOString() : new Date().toISOString(),
      remarks: formVal.remarks?.trim() || undefined,
      totalAmount: this.getFormGrandTotal(),
      items: itemsReq
    };

    this.receivingService.createReceiving(request).subscribe({
      next: (created) => {
        this.isSubmitting.set(false);
        this.isCreateModalOpen.set(false);
        this.showToast(`Purchase receipt ${created.receivingNumber} recorded successfully! Inventory updated.`);
        this.loadAllData();
      },
      error: (err) => {
        console.error('Failed to create purchase receipt:', err);
        this.isSubmitting.set(false);
        if (err.status === 409) {
          this.modalError.set(`Receipt #${formVal.receivingNumber} already exists. Duplicate numbers are not permitted.`);
        } else {
          this.modalError.set(err?.error?.message || err?.message || 'Failed to record purchase receipt.');
        }
      }
    });
  }

  deleteReceiving(receiving: PurchaseReceivingResponse, event: MouseEvent): void {
    event.stopPropagation();
    if ((receiving.status || '').toUpperCase() === 'RECEIVED') {
      alert('Received purchase receipts cannot be deleted because they affect inventory history.');
      return;
    }

    if (!confirm(`Are you sure you want to delete purchase receiving ${receiving.receivingNumber}?`)) {
      return;
    }

    this.receivingService.deleteReceiving(receiving.id).subscribe({
      next: () => {
        this.showToast(`Purchase receipt ${receiving.receivingNumber} deleted.`);
        this.loadAllData();
      },
      error: (err) => {
        console.error('Failed to delete receiving:', err);
        const msg = err?.error?.message || err?.message || 'Failed to delete purchase receipt.';
        alert(msg);
      }
    });
  }

  getStatusBadgeVariant(status: string): BadgeVariant {
    switch ((status || '').toUpperCase()) {
      case 'RECEIVED':
        return 'success';
      case 'PENDING':
        return 'warning';
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
