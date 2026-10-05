package com.fintrack.repository;

import com.fintrack.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository
        extends JpaRepository<Transfer, Long> {

    List<Transfer>
    findByUserIdOrderByTransferDateDesc(
            Long userId
    );

    Optional<Transfer>
    findByIdAndUserId(
            Long id,
            Long userId
    );
}