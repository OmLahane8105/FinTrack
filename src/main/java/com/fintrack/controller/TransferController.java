package com.fintrack.controller;

import com.fintrack.dto.TransferRequest;
import com.fintrack.dto.TransferResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(
            TransferService transferService
    ) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferResponse createTransfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return transferService.createTransfer(
                request,
                user.getUserId()
        );
    }

    @GetMapping
    public List<TransferResponse> getTransfers(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return transferService.getTransfers(
                user.getUserId()
        );
    }

    @DeleteMapping("/{id}")
    public void deleteTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        transferService.deleteTransfer(
                id,
                user.getUserId()
        );
    }
}