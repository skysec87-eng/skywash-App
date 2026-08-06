package com.skywash.api.config;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus()).body(Map.of(
        "error", ex.getMessage(),
        "status", ex.getStatus().value(),
        "timestamp", Instant.now().toString()
    ));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .orElse("Validation failed");
    return ResponseEntity.badRequest().body(Map.of(
        "error", msg,
        "status", 400,
        "timestamp", Instant.now().toString()
    ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
        "error", ex.getMessage() == null ? "Internal server error" : ex.getMessage(),
        "status", 500,
        "timestamp", Instant.now().toString()
    ));
  }
}
