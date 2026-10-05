package com.fintrack.repository;

import com.fintrack.entity.Account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    Optional<Account> findByIdAndUserId(
            Long id,
            Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT a
    FROM Account a
    WHERE a.id = :accountId
      AND a.user.id = :userId
""")
    Optional<Account> findByIdAndUserIdForUpdate(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId
    );

    @Query("""
           SELECT COALESCE(SUM(a.balance), 0)
           FROM Account a
           WHERE a.user.id = :userId
           """)
    BigDecimal getTotalBalance(
            @Param("userId") Long userId
    );

    @Query("""
       SELECT COALESCE(SUM(a.balance), 0)
       FROM Account a
       WHERE a.user.id = :userId
       AND a.type <> com.fintrack.entity.AccountType.CREDIT_CARD
       """)
    BigDecimal getTotalAssets(
            @Param("userId") Long userId
    );

    @Query("""
       SELECT COALESCE(SUM(a.balance), 0)
       FROM Account a
       WHERE a.user.id = :userId
       AND a.type = com.fintrack.entity.AccountType.CREDIT_CARD
       """)
    BigDecimal getCreditCardBalance(
            @Param("userId") Long userId
    );
}