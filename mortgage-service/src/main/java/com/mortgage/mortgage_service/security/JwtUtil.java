package com.mortgage.mortgage_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ─── 1. GENERATE TOKEN ─────────────────────────────────────────────────────
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);  // embed role inside token payload

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)               // who the token is about
                .setIssuedAt(new Date())            // when issued
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // sign with secret
                .compact();
    }

    // ─── 2. VALIDATE TOKEN ─────────────────────────────────────────────────────
    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }

    // ─── 3. EXTRACT USERNAME (subject) ─────────────────────────────────────────
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ─── 4. EXTRACT ROLE ───────────────────────────────────────────────────────
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // ─── 5. GENERIC CLAIM EXTRACTOR ────────────────────────────────────────────
    // Function<Claims, T> lets you pass any claim extractor — reusable pattern
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ─── 6. EXTRACT ALL CLAIMS (parse + verify signature) ─────────────────────
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())   // verify signature using secret
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ─── 7. CHECK EXPIRY ───────────────────────────────────────────────────────
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // ─── 8. BUILD SIGNING KEY ──────────────────────────────────────────────────
    // Decodes hex secret → bytes → HMAC-SHA key
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}