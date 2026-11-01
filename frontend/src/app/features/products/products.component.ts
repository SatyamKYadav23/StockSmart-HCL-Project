import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { Product, ProductRequest, Category, Supplier } from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent,
  CurrencyFormatPipe
} from '../../shared';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    IconComponent,
    BadgeComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    CurrencyFormatPipe
  ],
  templateUrl: './products.component.html',
  styleUrl: './products.component.css'
})
export class ProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly fb = inject(FormBuilder);

  // Data signals
  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly suppliers = signal<Supplier[]>([]);

  // State signals
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to connect to the backend server.');

  // Search & Filter signals
  readonly searchQuery = signal('');
  readonly selectedCategoryId = signal<'ALL' | number>('ALL');

  // Modal & Form signals
  readonly isFormModalOpen = signal(false);
  readonly isEditMode = signal(false);
  readonly editingProductId = signal<number | null>(null);
  readonly isSubmitting = signal(false);
  readonly formErrorMessage = signal<string | null>(null);

  // Delete Confirmation Modal signals
  readonly isDeleteModalOpen = signal(false);
  readonly productToDelete = signal<Product | null>(null);
  readonly isDeleting = signal(false);

  // Toast feedback signal
  readonly toastMessage = signal<string | null>(null);
  private toastTimer: any = null;

  // Reactive Form
  productForm: FormGroup = this.fb.group({
    sku: ['', [Validators.required, Validators.maxLength(100)]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    categoryId: [null, [Validators.required]],
    supplierId: [null],
    price: [null, [Validators.required, Validators.min(0)]],
    costPrice: [null, [Validators.min(0)]],
    barcode: [''],
    reorderLevel: [0, [Validators.min(0)]]
  });

  // Filtered Products computed signal
  readonly filteredProducts = computed(() => {
    const list = this.products();
    const query = this.searchQuery().trim().toLowerCase();
    const catId = this.selectedCategoryId();

    return list.filter((p) => {
      const matchesSearch =
        !query ||
        p.name.toLowerCase().includes(query) ||
        p.sku.toLowerCase().includes(query) ||
        (p.categoryName && p.categoryName.toLowerCase().includes(query)) ||
        (p.barcode && p.barcode.toLowerCase().includes(query));

      const matchesCategory = catId === 'ALL' || p.categoryId === Number(catId);

      return matchesSearch && matchesCategory;
    });
  });

  ngOnInit(): void {
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.loadProducts();
    this.loadCategories();
    this.loadSuppliers();
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.productService.getProducts().subscribe({
      next: (data) => {
        this.products.set(data);
        this.isLoading.set(false);
      },
      error: (err: Error) => {
        this.errorMessage.set(err.message || 'Unable to retrieve products from server.');
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (cats) => {
        this.categories.set(cats);
        if (cats.length > 0 && !this.productForm.get('categoryId')?.value && !this.isEditMode()) {
          this.productForm.patchValue({ categoryId: cats[0].id });
        }
      },
      error: () => {} // Non-blocking
    });
  }

  quickCreateCategory(name: string = 'General Merchandise'): void {
    this.isSubmitting.set(true);
    this.productService.createCategory({ name, description: 'Default retail category' }).subscribe({
      next: (cat) => {
        this.isSubmitting.set(false);
        this.loadCategories();
        this.productForm.patchValue({ categoryId: cat.id });
        this.showToast(`Category "${cat.name}" created.`);
      },
      error: (err: Error) => {
        this.isSubmitting.set(false);
        this.formErrorMessage.set(err.message || 'Failed to create category.');
      }
    });
  }

  loadSuppliers(): void {
    this.productService.getSuppliers().subscribe({
      next: (sups) => this.suppliers.set(sups),
      error: () => {} // Non-blocking
    });
  }

  // --- Modal & Form Actions ---

  openAddModal(): void {
    this.loadCategories();
    this.isEditMode.set(false);
    this.editingProductId.set(null);
    this.formErrorMessage.set(null);
    this.productForm.reset({
      sku: '',
      name: '',
      description: '',
      categoryId: this.categories().length > 0 ? this.categories()[0].id : null,
      supplierId: null,
      price: null,
      costPrice: null,
      barcode: '',
      reorderLevel: 0
    });
    this.isFormModalOpen.set(true);
  }

  openEditModal(product: Product): void {
    this.loadCategories();
    this.isEditMode.set(true);
    this.editingProductId.set(product.id);
    this.formErrorMessage.set(null);
    this.productForm.reset({
      sku: product.sku,
      name: product.name,
      description: product.description || '',
      categoryId: product.categoryId,
      supplierId: product.supplierId,
      price: product.price,
      costPrice: product.costPrice,
      barcode: product.barcode || '',
      reorderLevel: product.reorderLevel ?? 0
    });
    this.isFormModalOpen.set(true);
  }

  closeFormModal(): void {
    if (this.isSubmitting()) return;
    this.isFormModalOpen.set(false);
    this.formErrorMessage.set(null);
  }

  onSubmitForm(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      this.formErrorMessage.set('Please fill in all required fields (Product Name, SKU, Category, Selling Price).');
      return;
    }

    const formValues = this.productForm.value;
    const request: ProductRequest = {
      sku: formValues.sku.trim(),
      name: formValues.name.trim(),
      description: formValues.description ? formValues.description.trim() : null,
      categoryId: Number(formValues.categoryId),
      supplierId: formValues.supplierId ? Number(formValues.supplierId) : null,
      price: Number(formValues.price),
      costPrice: formValues.costPrice !== null && formValues.costPrice !== '' ? Number(formValues.costPrice) : null,
      barcode: formValues.barcode ? formValues.barcode.trim() : null,
      reorderLevel: formValues.reorderLevel !== null && formValues.reorderLevel !== '' ? Number(formValues.reorderLevel) : 0
    };

    this.isSubmitting.set(true);
    this.formErrorMessage.set(null);

    if (this.isEditMode()) {
      const id = this.editingProductId()!;
      this.productService.updateProduct(id, request).subscribe({
        next: (updated) => {
          this.isSubmitting.set(false);
          this.closeFormModal();
          this.showToast(`Product "${updated.name}" updated successfully.`);
          this.loadProducts();
        },
        error: (err: Error) => {
          this.isSubmitting.set(false);
          this.formErrorMessage.set(err.message || 'Failed to update product.');
        }
      });
    } else {
      this.productService.createProduct(request).subscribe({
        next: (created) => {
          this.isSubmitting.set(false);
          this.closeFormModal();
          this.showToast(`Product "${created.name}" created successfully.`);
          this.loadProducts();
        },
        error: (err: Error) => {
          this.isSubmitting.set(false);
          this.formErrorMessage.set(err.message || 'Failed to create product.');
        }
      });
    }
  }

  // --- Delete Actions ---

  openDeleteModal(product: Product): void {
    this.productToDelete.set(product);
    this.isDeleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    if (this.isDeleting()) return;
    this.isDeleteModalOpen.set(false);
    this.productToDelete.set(null);
  }

  confirmDelete(): void {
    const product = this.productToDelete();
    if (!product) return;

    this.isDeleting.set(true);
    this.productService.deleteProduct(product.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.closeDeleteModal();
        this.showToast(`Product "${product.name}" deleted successfully.`);
        this.loadProducts();
      },
      error: (err: Error) => {
        this.isDeleting.set(false);
        this.closeDeleteModal();
        this.showToast(`Error deleting product: ${err.message}`);
      }
    });
  }

  showToast(msg: string): void {
    this.toastMessage.set(msg);
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.toastMessage.set(null);
    }, 3500);
  }

  onCategorySelect(val: string): void {
    if (val === 'ALL') {
      this.selectedCategoryId.set('ALL');
    } else {
      this.selectedCategoryId.set(Number(val));
    }
  }
}
