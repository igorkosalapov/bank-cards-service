package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;

public class CardStatusUpdateRequest {
    @NotBlank
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
