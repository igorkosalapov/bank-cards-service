package com.example.bankcards.controller;

import com.example.bankcards.dto.BalanceResponse;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class UserCardController {

    private final CardService cardService;
    private final UserService userService;

    public UserCardController(CardService cardService, UserService userService) {
        this.cardService = cardService;
        this.userService = userService;
    }

    @GetMapping
    public Page<CardResponse> listMine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "blockRequested", required = false) Boolean blockRequested,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        User owner = userService.getEntity(principal.getId());
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String query = (search != null && !search.isBlank()) ? search : q;
        return cardService.listMine(owner, query, status, blockRequested, pageable);
    }

    @GetMapping("/{id}")
    public CardResponse getMine(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User owner = userService.getEntity(principal.getId());
        return cardService.getMineResponse(id, owner);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User owner = userService.getEntity(principal.getId());
        return cardService.getBalance(id, owner);
    }

    @PostMapping("/{id}/block-request")
    public CardResponse requestBlock(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User owner = userService.getEntity(principal.getId());
        return cardService.requestBlock(id, owner);
    }
}
