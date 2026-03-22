package com.payments.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdempotencyKeyException.class)
    public ResponseEntity<Map<String, String>> handleIdempotencyKey(IdempotencyKeyException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "INVALID_IDEMPOTENCY_KEY",
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(InvalidTransitionException e) {
        return ResponseEntity.unprocessableEntity().body(Map.of(
            "error", "INVALID_STATE_TRANSITION",
            "message", e.getMessage()
        ));
    }
}