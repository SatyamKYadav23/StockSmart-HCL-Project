import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { CategoryService } from '../../core/services/category.service';
import { ProductService } from '../../core/services/product.service';
import { Category, CategoryRequest, EnrichedCategory, Product } from '../../core/models';
import {
  IconComponent,
  BadgeComponent,
  KpiCardComponent,
  LoadingSkeletonComponent,
  EmptyStateComponent,
  ErrorStateComponent
} from '../../shared';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    IconComponent,
    BadgeComponent,
    KpiCardComponent,
    LoadingSkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent
  ],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css'
})
export class CategoriesComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly productService = inject(ProductService);
  private readonly fb = inject(FormBuilder);

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly errorMessage = signal('Unable to retrieve categories from server.');

  readonly categories = signal<Category[]>([]);
  readonly allProducts = signal<Product[]>([]);

  // Search filter
  readonly searchQuery = signal('');

  // Modals state
  readonly isFormModalOpen = signal(false);
  readonly isEditing = signal(false);
  readonly editingCategoryId = signal<number | null>(null);
  readonly isSubmitting = signal(false);
  readonly formErrorMessage = signal<string | null>(null);

  // Delete modal state
  readonly isDeleteModalOpen = signal(false);
  readonly categoryToDelete = signal<EnrichedCategory | null>(null);
  readonly isDeleting = signal(false);
  readonly deleteErrorMessage = signal<string | null>(null);

  // Toast feedback
  readonly toastMessage = signal<string | null>(null);
  private toastTimer: any = null;

  categoryForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(255)]]
  });

  // Enriched categories computed with live product counts and search filter
  readonly enrichedCategories = computed<EnrichedCategory[]>(() => {
    const cats = this.categories();
    const prods = this.allProducts();
    const query = this.searchQuery().trim().toLowerCase();

    // Map each category to include products count
    const enriched = cats.map(cat => ({
      ...cat,
      productsCount: prods.filter(p => p.categoryId === cat.id).length
    }));

    if (!query) {
      return enriched;
    }

    return enriched.filter(c =>
      c.name.toLowerCase().includes(query) ||
      (c.description && c.description.toLowerCase().includes(query))
    );
  });

  // KPI Summary Metrics
  readonly stats = computed(() => {
    const cats = this.categories();
    const prods = this.allProducts();
    const totalClassified = prods.filter(p => p.categoryId != null).length;
    const emptyCount = cats.filter(c => !prods.some(p => p.categoryId === c.id)).length;

    return {
      totalCategories: cats.length,
      totalClassifiedProducts: totalClassified,
      emptyCategoriesCount: emptyCount
    };
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    forkJoin({
      categories: this.categoryService.getCategories(),
      products: this.productService.getProducts()
    }).subscribe({
      next: ({ categories, products }) => {
        this.categories.set(categories);
        this.allProducts.set(products);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.message || 'Failed to load categories and catalog data.');
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  // --- Modal & Form Actions ---

  openAddModal(): void {
    this.isEditing.set(false);
    this.editingCategoryId.set(null);
    this.formErrorMessage.set(null);
    this.categoryForm.reset({
      name: '',
      description: ''
    });
    this.isFormModalOpen.set(true);
  }

  openEditModal(category: EnrichedCategory): void {
    this.isEditing.set(true);
    this.editingCategoryId.set(category.id);
    this.formErrorMessage.set(null);
    this.categoryForm.reset({
      name: category.name,
      description: category.description || ''
    });
    this.isFormModalOpen.set(true);
  }

  closeFormModal(): void {
    this.isFormModalOpen.set(false);
    this.formErrorMessage.set(null);
  }

  submitForm(): void {
    if (this.isSubmitting()) return;

    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    const formVal = this.categoryForm.value;
    const nameTrimmed = (formVal.name || '').trim();

    if (!nameTrimmed) {
      this.categoryForm.get('name')?.setErrors({ required: true });
      this.categoryForm.get('name')?.markAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.formErrorMessage.set(null);

    const request: CategoryRequest = {
      name: nameTrimmed,
      description: formVal.description ? formVal.description.trim() : null
    };

    if (this.isEditing()) {
      const id = this.editingCategoryId();
      if (!id) return;

      this.categoryService.updateCategory(id, request).subscribe({
        next: (updated) => {
          this.isSubmitting.set(false);
          this.closeFormModal();
          this.showToast(`Category "${updated.name}" updated successfully.`);
          this.loadData();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.formErrorMessage.set(err?.message || 'Failed to update category.');
        }
      });
    } else {
      this.categoryService.createCategory(request).subscribe({
        next: (created) => {
          this.isSubmitting.set(false);
          this.closeFormModal();
          this.showToast(`Category "${created.name}" created successfully.`);
          this.loadData();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.formErrorMessage.set(err?.message || 'Failed to create category.');
        }
      });
    }
  }

  // --- Deletion Flow ---

  openDeleteModal(category: EnrichedCategory): void {
    this.categoryToDelete.set(category);
    this.deleteErrorMessage.set(null);
    this.isDeleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.categoryToDelete.set(null);
    this.deleteErrorMessage.set(null);
  }

  confirmDelete(): void {
    if (this.isDeleting()) return;

    const cat = this.categoryToDelete();
    if (!cat) return;

    if (cat.productsCount > 0) {
      this.deleteErrorMessage.set(
        `Cannot delete category "${cat.name}" because it is currently linked to ${cat.productsCount} active product(s). Please reassign or remove those products first.`
      );
      return;
    }

    this.isDeleting.set(true);
    this.deleteErrorMessage.set(null);

    this.categoryService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.closeDeleteModal();
        this.showToast(`Category "${cat.name}" deleted successfully.`);
        this.loadData();
      },
      error: (err) => {
        this.isDeleting.set(false);
        this.deleteErrorMessage.set(err?.message || 'Unable to delete category from server.');
      }
    });
  }

  // --- Toast notification ---
  private showToast(msg: string): void {
    this.toastMessage.set(msg);
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastTimer = setTimeout(() => {
      this.toastMessage.set(null);
    }, 4000);
  }
}
