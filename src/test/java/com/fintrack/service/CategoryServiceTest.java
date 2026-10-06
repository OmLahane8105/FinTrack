package com.fintrack.service;

import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.CategoryResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.User;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User user;
    private Category category;
    private CategoryRequest request;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test User",
                "test@example.com",
                "hashed-password"
        );

        setId(user, 1L);

        category = new Category(
                "Food",
                user
        );

        setId(category, 10L);

        request = new CategoryRequest();
        request.setName("Food");
    }

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Test
    void createCategory_shouldCreateCategorySuccessfully() {

        when(categoryRepository.existsByNameAndUserId(
                "Food", 1L
        )).thenReturn(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(category);

        CategoryResponse response =
                categoryService.createCategory(request, 1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Food", response.getName());

        verify(categoryRepository)
                .existsByNameAndUserId("Food", 1L);

        verify(userRepository)
                .findById(1L);

        verify(categoryRepository)
                .save(any(Category.class));
    }

    @Test
    void createCategory_shouldRejectDuplicateCategory() {

        when(categoryRepository.existsByNameAndUserId(
                "Food", 1L
        )).thenReturn(true);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> categoryService.createCategory(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Category already exists",
                exception.getMessage()
        );

        verify(categoryRepository)
                .existsByNameAndUserId("Food", 1L);

        verify(userRepository, never())
                .findById(anyLong());

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void createCategory_shouldRejectMissingUser() {

        when(categoryRepository.existsByNameAndUserId(
                "Food", 1L
        )).thenReturn(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> categoryService.createCategory(
                                request,
                                1L
                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(userRepository)
                .findById(1L);

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    // ---------------------------------------------------------
    // GET
    // ---------------------------------------------------------

    @Test
    void getCategories_shouldReturnUserCategories() {

        Category secondCategory =
                new Category("Transport", user);

        setId(secondCategory, 11L);

        when(categoryRepository.findByUserId(1L))
                .thenReturn(List.of(
                        category,
                        secondCategory
                ));

        List<CategoryResponse> response =
                categoryService.getCategories(1L);

        assertNotNull(response);
        assertEquals(2, response.size());

        assertEquals(10L, response.get(0).getId());
        assertEquals("Food", response.get(0).getName());

        assertEquals(11L, response.get(1).getId());
        assertEquals("Transport", response.get(1).getName());

        verify(categoryRepository)
                .findByUserId(1L);
    }

    @Test
    void getCategories_shouldReturnEmptyListWhenNoCategoriesExist() {

        when(categoryRepository.findByUserId(1L))
                .thenReturn(List.of());

        List<CategoryResponse> response =
                categoryService.getCategories(1L);

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(categoryRepository)
                .findByUserId(1L);
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    @Test
    void updateCategory_shouldUpdateCategorySuccessfully() {

        CategoryResponse expected =
                new CategoryResponse(10L, "Groceries");

        request.setName("Groceries");

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(category))
                .thenReturn(category);

        CategoryResponse response =
                categoryService.updateCategory(
                        10L,
                        request,
                        1L
                );

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Groceries", response.getName());

        assertEquals(
                "Groceries",
                category.getName()
        );

        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);

        verify(categoryRepository)
                .save(category);
    }

    @Test
    void updateCategory_shouldRejectMissingCategory() {

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> categoryService.updateCategory(
                                10L,
                                request,
                                1L
                        )
                );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    @Test
    void deleteCategory_shouldDeleteCategorySuccessfully() {

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategory(10L, 1L);

        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);

        verify(categoryRepository)
                .delete(category);
    }

    @Test
    void deleteCategory_shouldRejectMissingCategory() {

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> categoryService.deleteCategory(
                                10L,
                                1L
                        )
                );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);

        verify(categoryRepository, never())
                .delete(any(Category.class));
    }

    // ---------------------------------------------------------
    // HELPER
    // ---------------------------------------------------------

    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}