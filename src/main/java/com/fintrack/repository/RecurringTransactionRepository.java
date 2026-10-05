package com.fintrack.repository;

import com.fintrack.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction>
    findByActiveTrueAndNextExecutionDateLessThanEqual(
            LocalDate date
    );

    List<RecurringTransaction>
    findByUserIdOrderByNextExecutionDateAsc(
            Long userId
    );

    Optional<RecurringTransaction>
    findByIdAndUserId(
            Long id,
            Long userId
    );
}