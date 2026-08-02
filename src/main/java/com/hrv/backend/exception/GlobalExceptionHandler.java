package com.hrv.backend.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    record ErrorBody(int status, String error, Object message, Instant timestamp) {}

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorBody> notFound(ResourceNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> badRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorBody> badCredentials(BadCredentialsException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, errors);
    }

    private ResponseEntity<ErrorBody> body(HttpStatus status, Object message) {
        return ResponseEntity.status(status)
                .body(new ErrorBody(status.value(), status.getReasonPhrase(), message, Instant.now()));
    }
}
