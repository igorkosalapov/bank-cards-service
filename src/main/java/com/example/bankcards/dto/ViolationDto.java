package com.example.bankcards.dto;

public class ViolationDto {
    private String field;
    private String message;

    public ViolationDto() {
    }

    public ViolationDto(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
