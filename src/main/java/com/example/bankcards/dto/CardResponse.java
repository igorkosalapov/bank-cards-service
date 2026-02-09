package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardResponse {
    private Long id;
    private String maskedNumber;
    private String ownerUsername;
    private String cardholderName;
    private String expiration;
    private String status;
    private BigDecimal balance;
}
