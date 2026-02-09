package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardStatusUpdateRequest;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
public class AdminCardController {

    private final CardService cardService;

    @PostMapping
    public CardResponse create(@Valid @RequestBody CardCreateRequest req) {
        return cardService.create(req);
    }

    @GetMapping
    public Page<CardResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String query = (search != null && !search.isBlank()) ? search : q;
        return cardService.listAll(query, status, pageable);
    }

    @GetMapping("/{id}")
    public CardResponse get(@PathVariable Long id) {
        return cardService.getByIdAdmin(id);
    }

    @PatchMapping("/{id}/status")
    public CardResponse updateStatus(@PathVariable Long id, @Valid @RequestBody CardStatusUpdateRequest req) {
        return cardService.updateStatus(id, req.getStatus());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cardService.delete(id);
    }
}
