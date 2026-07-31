package com.example.bankcards.exception;

import com.example.bankcards.dto.ErrorResponse;
import com.example.bankcards.dto.ViolationDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorResponse body = base(HttpStatus.BAD_REQUEST, request, "Validation failed");
        body.setViolations(ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        ErrorResponse body = base(HttpStatus.BAD_REQUEST, request, "Validation failed");
        body.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        ErrorResponse body = base(HttpStatus.BAD_REQUEST, request, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        ErrorResponse body = base(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        ErrorResponse body = base(HttpStatus.FORBIDDEN, request, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse body = base(HttpStatus.FORBIDDEN, request, "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        ErrorResponse body = base(
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                "Unexpected server error"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    private ErrorResponse base(HttpStatus status, HttpServletRequest request, String message) {
        ErrorResponse body = new ErrorResponse();
        body.setStatus(status.value());
        body.setError(status.getReasonPhrase());
        body.setMessage(message);
        body.setPath(request.getRequestURI());
        return body;
    }

    private ViolationDto toViolation(FieldError fe) {
        return new ViolationDto(fe.getField(), fe.getDefaultMessage());
    }
}
