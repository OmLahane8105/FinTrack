package com.fintrack.controller;

import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.CategoryResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(
            CategoryService categoryService) {

        this.categoryService = categoryService;
    }

    @PostMapping
    public CategoryResponse createCategory(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return categoryService.createCategory(
                request,
                user.getUserId());
    }

    @GetMapping
    public List<CategoryResponse> getCategories(
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return categoryService.getCategories(
                user.getUserId());
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return categoryService.updateCategory(
                id,
                request,
                user.getUserId());
    }

    @DeleteMapping("/{id}")
    public String deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        categoryService.deleteCategory(
                id,
                user.getUserId());

        return "Category deleted successfully";
    }
}