package com.memorygame.controller;

import com.memorygame.dto.AuthRequest;
import com.memorygame.dto.AuthResponse;
import com.memorygame.dto.ErrorResponse;
import com.memorygame.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles player registration and login.
 *
 * POST /api/auth/register  — create a new account
 * POST /api/auth/login     — verify credentials and get back userId
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new player.
     *
     * Request body:  { "username": "alice", "password": "secret123" }
     * Success (201): { "userId": 1, "username": "alice", "displayName": "alice", "message": "..." }
     * Failure (400): { "error": "Username 'alice' is already taken." }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Log in an existing player.
     *
     * Request body:  { "username": "alice", "password": "secret123" }
     * Success (200): { "userId": 1, "username": "alice", "displayName": "alice", "message": "..." }
     * Failure (401): { "error": "Invalid username or password." }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        }
    }
}
