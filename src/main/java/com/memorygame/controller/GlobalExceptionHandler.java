package com.memorygame.controller;

import com.memorygame.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Catches exceptions thrown anywhere in a controller and converts them
 * to a consistent JSON error response like: { "error": "some message" }
 *
 * Without this, Spring would return its default HTML error page which is
 * not useful for an API client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid failures (e.g. blank username, password too short).
     * Collects all validation messages into a single comma-separated string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorResponse(messages));
    }

    /**
     * Catch-all for any unexpected exception so the client still gets JSON.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred: " + ex.getMessage()));
    }
}
