import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { LocationService } from '../../core/services/location.service';
import { InventoryService } from '../../core/services/inventory.service';
import {
  OrderResponse,
  OrderItemResponse,
  OrderRequest,
  OrderItemRequest,
  OrderStatus,
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
  ErrorStateComponent,
  CurrencyFormatPipe
} from '../../shared';

@Component({
  selector: 'app-orders',
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
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.css'
})
export class OrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly productService = inject(ProductService);
  private readonly locationService = inject(LocationService);
  private readonly inventoryService = inject(InventoryService);
  private readonly fb = inject(FormBuilder);

  // Raw data signals
  readonly rawOrders = signal<OrderResponse[]>([]);
  readonly products = signal<Product[]>([]);
  readonly locations = signal<Location[]>([]);
  readonly inventory = signal<InventoryResponse[]>([]);

  // State signals
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to retrieve orders from backend.');

  // Filter & Search signals
  readonly searchQuery = signal('');
  readonly statusFilter = signal<string>('ALL');

  // Details Modal signals
  readonly selectedOrder = signal<OrderResponse | null>(null);
  readonly isDetailsModalOpen = signal(false);

  // Create Modal signals
  readonly isCreateModalOpen = signal(false);
  readonly isSubmitting = signal(false);
  readonly modalError = signal<string | null>(null);
  readonly toastMessage = signal<string | null>(null);

  // Create Order Reactive Form
  readonly orderForm: FormGroup = this.fb.group({
    orderNumber: ['', [Validators.required]],
    status: ['COMPLETED', [Validators.required]],
    locationId: [null, [Validators.required]],
    customerName: [''],
    remarks: [''],
    items: this.fb.array([])
  });

  get itemsArray(): FormArray {
    return this.orderForm.get('items') as FormArray;
  }

  // Filtered Orders
  readonly filteredOrders = computed<OrderResponse[]>(() => {
    const list = this.rawOrders();
    const q = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    return list.filter((order) => {
      const num = (order.orderNumber || '').toLowerCase();
      const customer = (order.customerName || '').toLowerCase();
      const loc = (order.locationName || '').toLowerCase();
      const remarks = (order.remarks || '').toLowerCase();

      const matchesSearch =
        !q ||
        num.includes(q) ||
        customer.includes(q) ||
        loc.includes(q) ||
        remarks.includes(q);

      const matchesStatus = status === 'ALL' || order.status === status;

      return matchesSearch && matchesStatus;
    });
  });

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.orderService.getOrders().subscribe({
      next: (orders) => {
        // Sort newest first
        const sorted = [...orders].sort((a, b) => {
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
        this.rawOrders.set(sorted);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.errorMessage.set(err.error?.message || err.message || 'Failed to connect to orders repository.');
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

    this.inventoryService.getInventory().subscribe({
      next: (inv) => this.inventory.set(inv),
      error: () => {}
    });
  }

  // --- Details Modal Actions ---

  openDetailsModal(order: OrderResponse): void {
    this.selectedOrder.set(order);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    this.selectedOrder.set(null);
  }

  // --- Create Order Modal Actions ---

  openCreateModal(): void {
    this.modalError.set(null);
    this.isSubmitting.set(false);

    const defaultOrderNum = `ORD-${Date.now().toString().slice(-6)}`;
    const defaultLocation = this.locations().length > 0 ? this.locations()[0].id : null;

    this.orderForm.reset({
      orderNumber: defaultOrderNum,
      status: 'COMPLETED',
      locationId: defaultLocation,
      customerName: 'Counter Retail Sale',
      remarks: ''
    });

    this.itemsArray.clear();

    // Add first default item row if products exist
    if (this.products().length > 0) {
      this.addItem(this.products()[0].id, this.products()[0].price);
    } else {
      this.addItem();
    }

    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.modalError.set(null);
    this.isSubmitting.set(false);
  }

  createItemRow(productId: number | null = null, unitPrice: number = 0): FormGroup {
    return this.fb.group({
      productId: [productId, [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(0)]]
    });
  }

  addItem(productId: number | null = null, unitPrice: number = 0): void {
    const row = this.createItemRow(productId, unitPrice);
    this.itemsArray.push(row);
  }

  removeItem(index: number): void {
    if (this.itemsArray.length > 1) {
      this.itemsArray.removeAt(index);
    }
  }

  onProductSelected(index: number): void {
    const row = this.itemsArray.at(index);
    const prodId = Number(row.get('productId')?.value);
    const prod = this.products().find(p => p.id === prodId);
    if (prod) {
      row.patchValue({ unitPrice: prod.price });
    }
  }

  // Live stock lookup
  getAvailableStock(productId: number | null, locationId: number | null): number {
    if (!productId || !locationId) return 0;
    const inv = this.inventory().find(i => i.productId === Number(productId) && i.locationId === Number(locationId));
    return inv ? inv.quantity : 0;
  }

  getItemSubtotal(index: number): number {
    const row = this.itemsArray.at(index);
    const qty = Number(row?.get('quantity')?.value) || 0;
    const price = Number(row?.get('unitPrice')?.value) || 0;
    return qty * price;
  }

  getOrderTotalAmount(): number {
    let total = 0;
    for (let i = 0; i < this.itemsArray.length; i++) {
      total += this.getItemSubtotal(i);
    }
    return total;
  }

  hasInsufficientStockForItem(index: number): boolean {
    const row = this.itemsArray.at(index);
    const status = this.orderForm.get('status')?.value;
    if (status !== 'COMPLETED') return false;

    const locId = Number(this.orderForm.get('locationId')?.value);
    const prodId = Number(row?.get('productId')?.value);
    const qty = Number(row?.get('quantity')?.value) || 0;

    const currentStock = this.getAvailableStock(prodId, locId);
    return qty > currentStock;
  }

  hasAnyStockError(): boolean {
    const status = this.orderForm.get('status')?.value;
    if (status !== 'COMPLETED') return false;

    for (let i = 0; i < this.itemsArray.length; i++) {
      if (this.hasInsufficientStockForItem(i)) {
        return true;
      }
    }
    return false;
  }

  submitOrder(): void {
    if (this.isSubmitting()) return;

    if (this.orderForm.invalid || this.hasAnyStockError()) {
      this.orderForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.modalError.set(null);

    const fv = this.orderForm.value;
    const itemsReq: OrderItemRequest[] = fv.items.map((it: any) => ({
      productId: Number(it.productId),
      quantity: Number(it.quantity),
      unitPrice: Number(it.unitPrice)
    }));

    const payload: OrderRequest = {
      orderNumber: fv.orderNumber.trim(),
      status: fv.status,
      customerName: fv.customerName?.trim() || undefined,
      locationId: fv.locationId ? Number(fv.locationId) : undefined,
      totalAmount: this.getOrderTotalAmount(),
      remarks: fv.remarks?.trim() || undefined,
      items: itemsReq
    };

    this.orderService.createOrder(payload).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.closeCreateModal();
        this.showToast(`Order ${res.orderNumber} successfully recorded with ${res.items?.length || 0} items.`);
        this.loadAllData();
      },
      error: (err: any) => {
        this.isSubmitting.set(false);
        const msg = err.error?.message || err.message || 'Failed to record order.';
        this.modalError.set(msg);
      }
    });
  }

  showToast(message: string): void {
    this.toastMessage.set(message);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 4500);
  }

  getOrderStatusVariant(status: string): BadgeVariant {
    switch (status) {
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
