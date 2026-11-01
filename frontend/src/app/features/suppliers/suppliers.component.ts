import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { SupplierService } from '../../core/services/supplier.service';
import { ProductService } from '../../core/services/product.service';
import { SupplierResponse, SupplierRequest, Product } from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent,
  CurrencyFormatPipe
} from '../../shared';

export interface EnrichedSupplier extends SupplierResponse {
  productsCount: number;
}

@Component({
  selector: 'app-suppliers',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    IconComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    CurrencyFormatPipe
  ],
  templateUrl: './suppliers.component.html',
  styleUrl: './suppliers.component.css'
})
export class SuppliersComponent implements OnInit {
  private readonly supplierService = inject(SupplierService);
  private readonly productService = inject(ProductService);
  private readonly fb = inject(FormBuilder);

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to load suppliers from the backend.');

  readonly suppliers = signal<SupplierResponse[]>([]);
  readonly allProducts = signal<Product[]>([]);

  // Search filter
  readonly searchQuery = signal('');

  // Modals state
  readonly isFormModalOpen = signal(false);
  readonly isEditing = signal(false);
  readonly editingSupplierId = signal<number | null>(null);
  readonly isSubmitting = signal(false);
  readonly formErrorMessage = signal<string | null>(null);

  readonly isDetailsModalOpen = signal(false);
  readonly selectedSupplier = signal<SupplierResponse | null>(null);

  readonly isDeleteModalOpen = signal(false);
  readonly supplierToDelete = signal<EnrichedSupplier | null>(null);
  readonly isDeleting = signal(false);
  readonly deleteErrorMessage = signal<string | null>(null);

  supplierForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    phone: ['', [Validators.maxLength(20)]],
    address: ['', [Validators.maxLength(255)]]
  });

  // Lookup map of supplierId -> Product[]
  readonly supplierProductsMap = computed(() => {
    const products = this.allProducts();
    const map = new Map<number, Product[]>();
    for (const p of products) {
      if (p.supplierId != null) {
        const list = map.get(p.supplierId) || [];
        list.push(p);
        map.set(p.supplierId, list);
      }
    }
    return map;
  });

  // Enriched suppliers with product counts
  readonly enrichedSuppliers = computed<EnrichedSupplier[]>(() => {
    const list = this.suppliers();
    const map = this.supplierProductsMap();
    return list.map(s => ({
      ...s,
      productsCount: map.get(s.id)?.length || 0
    }));
  });

  // Filtered suppliers based on search
  readonly filteredSuppliers = computed(() => {
    const list = this.enrichedSuppliers();
    const query = this.searchQuery().trim().toLowerCase();
    if (!query) return list;

    return list.filter(s =>
      s.name.toLowerCase().includes(query) ||
      (s.email && s.email.toLowerCase().includes(query)) ||
      (s.phone && s.phone.toLowerCase().includes(query)) ||
      (s.address && s.address.toLowerCase().includes(query))
    );
  });

  // Products belonging to currently viewed supplier
  readonly selectedSupplierProducts = computed<Product[]>(() => {
    const supplier = this.selectedSupplier();
    if (!supplier) return [];
    return this.supplierProductsMap().get(supplier.id) || [];
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    forkJoin({
      suppliers: this.supplierService.getSuppliers(),
      products: this.productService.getProducts()
    }).subscribe({
      next: ({ suppliers, products }) => {
        this.suppliers.set(suppliers);
        this.allProducts.set(products);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load supplier data:', err);
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  // --- Modal Openers ---
  openCreateModal(): void {
    this.isEditing.set(false);
    this.editingSupplierId.set(null);
    this.formErrorMessage.set(null);
    this.supplierForm.reset({
      name: '',
      email: '',
      phone: '',
      address: ''
    });
    this.isFormModalOpen.set(true);
  }

  openEditModal(supplier: SupplierResponse): void {
    this.isEditing.set(true);
    this.editingSupplierId.set(supplier.id);
    this.formErrorMessage.set(null);
    this.supplierForm.patchValue({
      name: supplier.name,
      email: supplier.email || '',
      phone: supplier.phone || '',
      address: supplier.address || ''
    });
    this.isFormModalOpen.set(true);
  }

  closeFormModal(): void {
    this.isFormModalOpen.set(false);
    this.formErrorMessage.set(null);
  }

  openDetailsModal(supplier: SupplierResponse): void {
    this.selectedSupplier.set(supplier);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    this.selectedSupplier.set(null);
  }

  openDeleteModal(supplier: EnrichedSupplier): void {
    this.supplierToDelete.set(supplier);
    this.deleteErrorMessage.set(null);
    this.isDeleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.supplierToDelete.set(null);
    this.deleteErrorMessage.set(null);
  }

  // --- Actions ---
  onSubmitForm(): void {
    if (this.isSubmitting()) return;

    if (this.supplierForm.invalid) {
      this.supplierForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.formErrorMessage.set(null);

    const val = this.supplierForm.value;
    const request: SupplierRequest = {
      name: val.name.trim(),
      email: val.email ? val.email.trim() : null,
      phone: val.phone ? val.phone.trim() : null,
      address: val.address ? val.address.trim() : null
    };

    if (this.isEditing()) {
      const id = this.editingSupplierId()!;
      this.supplierService.updateSupplier(id, request).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeFormModal();
          this.loadData();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const msg = err.error?.message || 'Failed to update supplier. Please verify entered values.';
          this.formErrorMessage.set(msg);
        }
      });
    } else {
      this.supplierService.createSupplier(request).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeFormModal();
          this.loadData();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const msg = err.error?.message || 'Failed to register supplier. Please verify entered values.';
          this.formErrorMessage.set(msg);
        }
      });
    }
  }

  confirmDelete(): void {
    const supplier = this.supplierToDelete();
    if (!supplier) return;

    if (supplier.productsCount > 0) {
      this.deleteErrorMessage.set(
        `Cannot delete "${supplier.name}" because ${supplier.productsCount} catalog product(s) are currently assigned to this vendor. Reassign or remove products first.`
      );
      return;
    }

    this.isDeleting.set(true);
    this.deleteErrorMessage.set(null);

    this.supplierService.deleteSupplier(supplier.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.closeDeleteModal();
        this.loadData();
      },
      error: (err) => {
        this.isDeleting.set(false);
        const msg = err.error?.message || 'Cannot delete supplier because it is referenced by existing catalog products.';
        this.deleteErrorMessage.set(msg);
      }
    });
  }
}
