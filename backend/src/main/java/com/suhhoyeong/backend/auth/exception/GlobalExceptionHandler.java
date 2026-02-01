package com.suhhoyeong.backend.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException e, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(errorBody(HttpStatus.UNAUTHORIZED.value(), e.getMessage(), req));
  }

  @ExceptionHandler(WrongProviderException.class)
  public ResponseEntity<?> handleWrongProvider(WrongProviderException e, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(errorBody(HttpStatus.UNAUTHORIZED.value(), "use social login", req));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleGeneric(Exception e, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), "internal error", req));
  }

  private Map<String, Object> errorBody(int status, String message, HttpServletRequest req) {
    return Map.of(
        "timestamp", Instant.now().toString(),
        "status", status,
        "error", message,
        "path", req.getRequestURI());
  }
}
