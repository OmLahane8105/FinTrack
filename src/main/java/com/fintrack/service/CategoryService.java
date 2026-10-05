package com.fintrack.service;

import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.CategoryResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.User;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository) {

        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public CategoryResponse createCategory(
            CategoryRequest request,
            Long userId) {

        if (categoryRepository.existsByNameAndUserId(
                request.getName(),
                userId)) {

            throw new RuntimeException(
                    "Category already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Category category = new Category(
                request.getName(),
                user
        );

        Category saved =
                categoryRepository.save(category);

        return toResponse(saved);
    }

    public List<CategoryResponse> getCategories(
            Long userId) {

        return categoryRepository
                .findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse updateCategory(
            Long categoryId,
            CategoryRequest request,
            Long userId) {

        Category category =
                categoryRepository
                        .findByIdAndUserId(
                                categoryId,
                                userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found"));

        category.setName(request.getName());

        return toResponse(
                categoryRepository.save(category));
    }

    public void deleteCategory(
            Long categoryId,
            Long userId) {

        Category category =
                categoryRepository
                        .findByIdAndUserId(
                                categoryId,
                                userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found"));

        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(
            Category category) {

        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}