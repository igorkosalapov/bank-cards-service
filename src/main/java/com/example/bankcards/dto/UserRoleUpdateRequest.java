package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;

public class UserRoleUpdateRequest {
    @NotBlank
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
