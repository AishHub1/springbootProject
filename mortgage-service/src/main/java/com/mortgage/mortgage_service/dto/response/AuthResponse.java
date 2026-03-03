package com.mortgage.mortgage_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;         // JWT token — client stores this
    private String tokenType;     // Always "Bearer"
    private String email;
    private String role;
    private long expiresIn;       // milliseconds — client knows when to re-login

    // ─── Static factory — clean way to build response ─────────────────────────
    public static AuthResponse of(String token, String email, String role, long expiresIn) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(email)
                .role(role)
                .expiresIn(expiresIn)
                .build();
    }
}