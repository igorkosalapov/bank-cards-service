package com.example.bankcards.util;

import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Transfer;

public final class TransferMapper {

    private TransferMapper() {
    }

    public static TransferResponse toResponse(Transfer t) {
        TransferResponse dto = new TransferResponse();
        dto.setId(t.getId());
        dto.setFromCardId(t.getFromCard().getId());
        dto.setToCardId(t.getToCard().getId());
        dto.setAmount(t.getAmount());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }
}
