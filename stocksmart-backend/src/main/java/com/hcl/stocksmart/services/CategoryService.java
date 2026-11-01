package com.hcl.stocksmart.services;

import com.hcl.stocksmart.dto.CategoryRequest;
import com.hcl.stocksmart.entity.Category;
import com.hcl.stocksmart.exception.DuplicateResourceException;
import com.hcl.stocksmart.exception.InvalidStockOperationException;
import com.hcl.stocksmart.exception.ResourceNotFoundException;
import com.hcl.stocksmart.repository.CategoryRepository;
import com.hcl.stocksmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;


    @Transactional
    public Category createCategory(CategoryRequest request) {

        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(
                    "Category already exists with name: " + request.name()
            );
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        return categoryRepository.save(category);
    }


    public List<Category> getAllCategories() {

        return categoryRepository.findAll();
    }


    public Category getCategoryById(Long id) {

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found with id: " + id
                        )
                );
    }


    @Transactional
    public Category updateCategory(
            Long id,
            CategoryRequest request) {

        Category category = getCategoryById(id);

        if (!category.getName().equals(request.name())
                && categoryRepository.existsByName(request.name())) {

            throw new DuplicateResourceException(
                    "Category already exists with name: "
                            + request.name()
            );
        }

        category.setName(request.name());
        category.setDescription(request.description());

        return categoryRepository.save(category);
    }


    @Transactional
    public void deleteCategory(Long id) {

        Category category = getCategoryById(id);

        if (productRepository.existsByCategoryId(id)) {
            throw new InvalidStockOperationException(
                    "Cannot delete category '" + category.getName() + "' because it is associated with existing products."
            );
        }

        categoryRepository.delete(category);
    }
}