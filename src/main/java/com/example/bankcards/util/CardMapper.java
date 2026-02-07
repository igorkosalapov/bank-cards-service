package com.example.bankcards.util;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;

public final class CardMapper {

    private CardMapper() {
    }

    public static CardResponse toResponse(Card card, boolean includeOwner) {
        CardResponse dto = new CardResponse();
        dto.setId(card.getId());
        dto.setMaskedNumber(CardNumberUtil.maskByLast4(card.getLast4()));
        if (includeOwner) {
            dto.setOwnerUsername(card.getOwner().getUsername());
        }
        dto.setCardholderName(card.getCardholderName());
        dto.setExpiration(ExpirationUtil.formatToMmYy(card.getExpirationDate()));
        dto.setStatus(card.getStatus().name());
        dto.setBalance(card.getBalance());
        dto.setBlockRequested(card.isBlockRequested());
        dto.setCreatedAt(card.getCreatedAt());
        return dto;
    }
}
