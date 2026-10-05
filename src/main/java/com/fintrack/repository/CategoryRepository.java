package com.fintrack.repository;

import com.fintrack.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);

    Optional<Category> findByIdAndUserId(
            Long id,
            Long userId);

    boolean existsByNameAndUserId(
            String name,
            Long userId);
}