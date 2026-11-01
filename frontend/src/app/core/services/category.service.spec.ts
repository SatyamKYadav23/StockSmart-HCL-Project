import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CategoryService } from './category.service';
import { Category, CategoryRequest } from '../models';
import { environment } from '../../../environments/environment';

describe('CategoryService', () => {
  let service: CategoryService;
  let httpTestingController: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CategoryService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CategoryService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all categories via GET /api/categories', () => {
    const mockCategories: Category[] = [
      { id: 1, name: 'Electronics', description: 'Gadgets and hardware' },
      { id: 2, name: 'Apparel', description: 'Clothing and accessories' }
    ];

    service.getCategories().subscribe((cats) => {
      expect(cats.length).toBe(2);
      expect(cats).toEqual(mockCategories);
    });

    const req = httpTestingController.expectOne(`${baseUrl}/categories`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCategories);
  });

  it('should get category by id via GET /api/categories/:id', () => {
    const mockCategory: Category = { id: 1, name: 'Electronics' };

    service.getCategoryById(1).subscribe((cat) => {
      expect(cat).toEqual(mockCategory);
    });

    const req = httpTestingController.expectOne(`${baseUrl}/categories/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCategory);
  });

  it('should create category via POST /api/categories', () => {
    const newCategoryReq: CategoryRequest = { name: 'Home Goods', description: 'Furniture & Decor' };
    const createdCategory: Category = { id: 3, name: 'Home Goods', description: 'Furniture & Decor' };

    service.createCategory(newCategoryReq).subscribe((cat) => {
      expect(cat).toEqual(createdCategory);
    });

    const req = httpTestingController.expectOne(`${baseUrl}/categories`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newCategoryReq);
    req.flush(createdCategory);
  });

  it('should update category via PUT /api/categories/:id', () => {
    const updateReq: CategoryRequest = { name: 'Electronics & Audio' };
    const updatedCategory: Category = { id: 1, name: 'Electronics & Audio' };

    service.updateCategory(1, updateReq).subscribe((cat) => {
      expect(cat).toEqual(updatedCategory);
    });

    const req = httpTestingController.expectOne(`${baseUrl}/categories/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updateReq);
    req.flush(updatedCategory);
  });

  it('should delete category via DELETE /api/categories/:id', () => {
    service.deleteCategory(1).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpTestingController.expectOne(`${baseUrl}/categories/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
