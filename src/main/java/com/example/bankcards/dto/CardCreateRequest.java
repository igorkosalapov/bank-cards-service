package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateRequest {

    @NotNull
    private Long ownerId;

    @NotBlank
    @Pattern(regexp = "\\d{16}", message = "cardNumber must be 16 digits")
    private String cardNumber;

    private String cardholderName;

    @NotBlank
    @Pattern(
            regexp = "^(\\d{4}-(0[1-9]|1[0-2])|(0[1-9]|1[0-2])/\\d{2})$",
            message = "expiration must be in format yyyy-MM or MM/YY"
    )
    private String expiration;

    private BigDecimal initialBalance;
}
