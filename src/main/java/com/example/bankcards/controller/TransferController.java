package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferResponse create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TransferRequest req) {
        return transferService.transferBetweenOwnCards(principal, req);
    }

    @GetMapping
    public org.springframework.data.domain.Page<TransferResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return transferService.listMyTransfers(principal, page, size);
    }
}
