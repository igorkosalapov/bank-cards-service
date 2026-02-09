package com.example.bankcards.util;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CardMapper {

    public static CardResponse toResponse(Card card, CardStatus effectiveStatus, boolean includeOwner) {
        CardResponse dto = new CardResponse();
        dto.setId(card.getId());
        dto.setMaskedNumber(CardNumberUtil.maskByLast4(card.getLast4()));
        if (includeOwner) {
            dto.setOwnerUsername(card.getOwner().getUsername());
        }
        dto.setCardholderName(card.getCardholderName());
        dto.setExpiration(ExpirationUtil.formatToMmYy(card.getExpirationDate()));
        dto.setStatus(effectiveStatus.name());
        dto.setBalance(card.getBalance());
        return dto;
    }

    public static CardResponse toResponse(Card card, boolean includeOwner) {
        CardStatus st = card.getStatus() == null ? CardStatus.ACTIVE : card.getStatus();
        return toResponse(card, st, includeOwner);
    }
}
