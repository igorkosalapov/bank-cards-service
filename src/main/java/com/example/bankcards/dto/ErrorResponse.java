package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private Instant timestamp = Instant.now();
    private int status;
    private String error;
    private String message;
    private String path;
    private List<ViolationDto> violations = new ArrayList<>();
}
