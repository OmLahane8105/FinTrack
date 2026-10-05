package com.fintrack.repository;

import com.fintrack.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(
            Long id,
            Long userId
    );

    List<Transaction> findTop10ByUserIdOrderByTransactionDateDesc(
            Long userId
    );

    List<Transaction> findByUserIdOrderByTransactionDateDesc(
            Long userId
    );

    Page<Transaction> findByUserId(
            Long userId,
            Pageable pageable
    );


    @Query("""
       SELECT
           t.category.id,
           t.category.name,
           COALESCE(SUM(t.amount), 0)
       FROM Transaction t
       WHERE t.user.id = :userId
       AND t.type = com.fintrack.entity.TransactionType.EXPENSE
       AND t.transactionDate BETWEEN :from AND :to
       GROUP BY t.category.id, t.category.name
       ORDER BY SUM(t.amount) DESC
       """)
    List<Object[]> getExpensesByCategory(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(value = """
       SELECT
           EXTRACT(YEAR FROM transaction_date)::int AS year,
           EXTRACT(MONTH FROM transaction_date)::int AS month,

           COALESCE(
               SUM(
                   CASE
                       WHEN type = 'INCOME'
                       THEN amount
                       ELSE 0
                   END
               ), 0
           ) AS income,

           COALESCE(
               SUM(
                   CASE
                       WHEN type = 'EXPENSE'
                       THEN amount
                       ELSE 0
                   END
               ), 0
           ) AS expenses

       FROM transactions

       WHERE user_id = :userId
       AND transaction_date BETWEEN :from AND :to

       GROUP BY
           EXTRACT(YEAR FROM transaction_date),
           EXTRACT(MONTH FROM transaction_date)

       ORDER BY
           year DESC,
           month DESC
       """,
            nativeQuery = true)
    List<Object[]> getMonthlySummary(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM Transaction t
       WHERE t.user.id = :userId
       AND t.type = com.fintrack.entity.TransactionType.INCOME
       AND t.transactionDate BETWEEN :from AND :to
       """)
    BigDecimal getTotalIncome(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM Transaction t
       WHERE t.user.id = :userId
       AND t.type = com.fintrack.entity.TransactionType.EXPENSE
       AND t.transactionDate BETWEEN :from AND :to
       """)
    BigDecimal getTotalExpenses(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM Transaction t
    WHERE t.user.id = :userId
      AND t.category.id = :categoryId
      AND t.type = com.fintrack.entity.TransactionType.EXPENSE
      AND t.transactionDate >= :startDate
      AND t.transactionDate <= :endDate
""")
    BigDecimal getExpenseTotalForCategoryAndDateRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}