package com.example.bankcards.dto;

public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private long expiresInMinutes;

    public AuthResponse() {
    }

    public AuthResponse(String token, long expiresInMinutes) {
        this.token = token;
        this.expiresInMinutes = expiresInMinutes;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(long expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
}
