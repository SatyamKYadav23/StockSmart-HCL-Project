import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { CategoriesComponent } from './categories.component';
import { CategoryService } from '../../core/services/category.service';
import { ProductService } from '../../core/services/product.service';
import { Category, Product, EnrichedCategory } from '../../core/models';

describe('CategoriesComponent', () => {
  let component: CategoriesComponent;
  let fixture: ComponentFixture<CategoriesComponent>;
  let mockCategoryService: {
    getCategories: ReturnType<typeof vi.fn>;
    createCategory: ReturnType<typeof vi.fn>;
    updateCategory: ReturnType<typeof vi.fn>;
    deleteCategory: ReturnType<typeof vi.fn>;
  };
  let mockProductService: {
    getProducts: ReturnType<typeof vi.fn>;
  };

  const initialCategories: Category[] = [
    { id: 1, name: 'Electronics', description: 'Gadgets' },
    { id: 2, name: 'Office Supplies', description: 'Stationery' },
    { id: 3, name: 'Hardware', description: 'Tools and screws' }
  ];

  const initialProducts: Partial<Product>[] = [
    { id: 101, name: 'Laptop', sku: 'LAP-001', categoryId: 1, price: 999 },
    { id: 102, name: 'Mouse', sku: 'MOU-001', categoryId: 1, price: 29 },
    { id: 103, name: 'Notebook', sku: 'NB-001', categoryId: 2, price: 5 }
  ];

  beforeEach(async () => {
    mockCategoryService = {
      getCategories: vi.fn().mockReturnValue(of(initialCategories)),
      createCategory: vi.fn(),
      updateCategory: vi.fn(),
      deleteCategory: vi.fn()
    };
    mockProductService = {
      getProducts: vi.fn().mockReturnValue(of(initialProducts as Product[]))
    };

    await TestBed.configureTestingModule({
      imports: [CategoriesComponent],
      providers: [
        { provide: CategoryService, useValue: mockCategoryService },
        { provide: ProductService, useValue: mockProductService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the categories component and load initial data', () => {
    expect(component).toBeTruthy();
    expect(component.categories().length).toBe(3);
    expect(component.allProducts().length).toBe(3);
    expect(component.isLoading()).toBe(false);
  });

  it('should enrich categories with correct product counts', () => {
    const enriched = component.enrichedCategories();
    expect(enriched.length).toBe(3);

    const electronics = enriched.find((c) => c.name === 'Electronics');
    const office = enriched.find((c) => c.name === 'Office Supplies');
    const hardware = enriched.find((c) => c.name === 'Hardware');

    expect(electronics?.productsCount).toBe(2);
    expect(office?.productsCount).toBe(1);
    expect(hardware?.productsCount).toBe(0);
  });

  it('should calculate stats correctly', () => {
    const stats = component.stats();
    expect(stats.totalCategories).toBe(3);
    expect(stats.totalClassifiedProducts).toBe(3);
    expect(stats.emptyCategoriesCount).toBe(1); // Hardware has 0 products
  });

  it('should filter categories by search query', () => {
    component.searchQuery.set('Office');
    const enriched = component.enrichedCategories();
    expect(enriched.length).toBe(1);
    expect(enriched[0].name).toBe('Office Supplies');
  });

  it('should open and initialize add category modal', () => {
    component.openAddModal();
    expect(component.isFormModalOpen()).toBe(true);
    expect(component.isEditing()).toBe(false);
    expect(component.categoryForm.get('name')?.value).toBe('');
  });

  it('should validate required name in form', () => {
    component.openAddModal();
    component.categoryForm.patchValue({ name: '   ' }); // whitespace only
    component.submitForm();

    expect(mockCategoryService.createCategory).not.toHaveBeenCalled();
    expect(component.categoryForm.get('name')?.hasError('required')).toBe(true);
  });

  it('should call createCategory on valid add submission', () => {
    const newCat: Category = { id: 4, name: 'Books', description: 'Paperback & Hardcover' };
    mockCategoryService.createCategory.mockReturnValue(of(newCat));

    component.openAddModal();
    component.categoryForm.patchValue({ name: 'Books', description: 'Paperback & Hardcover' });
    component.submitForm();

    expect(mockCategoryService.createCategory).toHaveBeenCalledWith({
      name: 'Books',
      description: 'Paperback & Hardcover'
    });
    expect(component.isFormModalOpen()).toBe(false);
  });

  it('should open edit modal with category data and submit update', () => {
    const targetCat: EnrichedCategory = {
      id: 1,
      name: 'Electronics',
      description: 'Gadgets',
      productsCount: 2
    };

    const updatedCat: Category = {
      id: 1,
      name: 'Consumer Electronics',
      description: 'Gadgets & Home Audio'
    };
    mockCategoryService.updateCategory.mockReturnValue(of(updatedCat));

    component.openEditModal(targetCat);
    expect(component.isFormModalOpen()).toBe(true);
    expect(component.isEditing()).toBe(true);
    expect(component.categoryForm.get('name')?.value).toBe('Electronics');

    component.categoryForm.patchValue({
      name: 'Consumer Electronics',
      description: 'Gadgets & Home Audio'
    });
    component.submitForm();

    expect(mockCategoryService.updateCategory).toHaveBeenCalledWith(1, {
      name: 'Consumer Electronics',
      description: 'Gadgets & Home Audio'
    });
  });

  it('should prevent delete if category has productsCount > 0', () => {
    const catWithProducts: EnrichedCategory = {
      id: 1,
      name: 'Electronics',
      description: 'Gadgets',
      productsCount: 2
    };

    component.openDeleteModal(catWithProducts);
    component.confirmDelete();

    expect(mockCategoryService.deleteCategory).not.toHaveBeenCalled();
    expect(component.deleteErrorMessage()).toContain('Cannot delete category "Electronics"');
  });

  it('should execute delete if category has productsCount === 0', () => {
    const emptyCat: EnrichedCategory = {
      id: 3,
      name: 'Hardware',
      description: 'Tools and screws',
      productsCount: 0
    };

    mockCategoryService.deleteCategory.mockReturnValue(of(void 0));

    component.openDeleteModal(emptyCat);
    component.confirmDelete();

    expect(mockCategoryService.deleteCategory).toHaveBeenCalledWith(3);
    expect(component.isDeleteModalOpen()).toBe(false);
  });
});
