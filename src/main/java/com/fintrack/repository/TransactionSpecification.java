package com.fintrack.repository;

import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> hasUserId(
            Long userId) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("user").get("id"),
                        userId
                );
    }

    public static Specification<Transaction> hasType(
            TransactionType type) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("type"),
                        type
                );
    }

    public static Specification<Transaction> hasCategoryId(
            Long categoryId) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("category").get("id"),
                        categoryId
                );
    }

    public static Specification<Transaction> dateAfterOrEqual(
            LocalDate date) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("transactionDate"),
                        date
                );
    }

    public static Specification<Transaction> dateBeforeOrEqual(
            LocalDate date) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("transactionDate"),
                        date
                );
    }

    public static Specification<Transaction> descriptionContains(
            String search) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("description")
                        ),
                        "%" + search.toLowerCase() + "%"
                );
    }
}