package com.mortgage.mortgage_service.controller;

import com.mortgage.mortgage_service.dto.request.LoginRequest;
import com.mortgage.mortgage_service.dto.request.RegisterRequest;
import com.mortgage.mortgage_service.dto.response.AuthResponse;
import com.mortgage.mortgage_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─── REGISTER ──────────────────────────────────────────────────────────────
    // Public endpoint — no JWT needed
    // @Valid triggers Bean Validation on RegisterRequest fields
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── LOGIN ─────────────────────────────────────────────────────────────────
    // Public endpoint — validates credentials, returns JWT token
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}