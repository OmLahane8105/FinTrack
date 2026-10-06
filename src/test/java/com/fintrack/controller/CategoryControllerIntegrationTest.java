package com.fintrack.controller;

import com.fintrack.dto.CategoryRequest;
import com.fintrack.dto.CategoryResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.CategoryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {

        CategoryController categoryController =
                new CategoryController(categoryService);

        CustomUserPrincipal principal =
                createPrincipal();

        HandlerMethodArgumentResolver principalResolver =
                new HandlerMethodArgumentResolver() {

                    @Override
                    public boolean supportsParameter(
                            MethodParameter parameter) {

                        return parameter.getParameterType()
                                .equals(CustomUserPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory) {

                        return principal;
                    }
                };

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(categoryController)
                        .setCustomArgumentResolvers(
                                principalResolver
                        )
                        .build();
    }

    private CustomUserPrincipal createPrincipal() {

        User user =
                new User(
                        "Test User",
                        "test@example.com",
                        "password"
                );

        user.setId(1L);

        return new CustomUserPrincipal(user);
    }

    @Test
    void createCategory_shouldReturnCreatedCategory()
            throws Exception {

        CategoryResponse response =
                new CategoryResponse(
                        1L,
                        "Food"
                );

        when(categoryService.createCategory(
                any(CategoryRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/categories")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "Food"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Food"));

        verify(categoryService)
                .createCategory(
                        any(CategoryRequest.class),
                        eq(1L)
                );
    }

    @Test
    void getCategories_shouldReturnCategories()
            throws Exception {

        List<CategoryResponse> categories =
                List.of(
                        new CategoryResponse(
                                1L,
                                "Food"
                        ),
                        new CategoryResponse(
                                2L,
                                "Transport"
                        ),
                        new CategoryResponse(
                                3L,
                                "Entertainment"
                        )
                );

        when(categoryService.getCategories(1L))
                .thenReturn(categories);

        mockMvc.perform(
                        get("/api/categories")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Transport"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].name")
                        .value("Entertainment"));

        verify(categoryService)
                .getCategories(1L);
    }

    @Test
    void updateCategory_shouldReturnUpdatedCategory()
            throws Exception {

        CategoryResponse response =
                new CategoryResponse(
                        1L,
                        "Groceries"
                );

        when(categoryService.updateCategory(
                eq(1L),
                any(CategoryRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/categories/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "Groceries"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Groceries"));

        verify(categoryService)
                .updateCategory(
                        eq(1L),
                        any(CategoryRequest.class),
                        eq(1L)
                );
    }

    @Test
    void deleteCategory_shouldReturnSuccessMessage()
            throws Exception {

        doNothing()
                .when(categoryService)
                .deleteCategory(1L, 1L);

        mockMvc.perform(
                        delete("/api/categories/1")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                "Category deleted successfully"
                        )
                );

        verify(categoryService)
                .deleteCategory(1L, 1L);
    }

    @Test
    void createCategory_withBlankName_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/categories")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_withBlankName_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        put("/api/categories/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}