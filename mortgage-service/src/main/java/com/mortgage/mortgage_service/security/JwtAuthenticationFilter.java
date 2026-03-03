package com.mortgage.mortgage_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// OncePerRequestFilter — guaranteed to run ONCE per request (not twice in forwards/includes)
// This filter intercepts every request, extracts JWT, validates it, sets SecurityContext

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ─── Step 1: Extract token from header ────────────────────────────────
        String token = extractTokenFromRequest(request);

        // ─── Step 2: If no token, skip — let Spring Security handle it ─────────
        // Public endpoints (login, register) have no token — that's fine
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ─── Step 3: Extract username from token ──────────────────────────────
        String username = null;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            // Malformed token — log and continue without setting auth
            log.warn("JWT token is malformed or invalid: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ─── Step 4: If username found AND not already authenticated ───────────
        // SecurityContextHolder.getContext().getAuthentication() == null
        // means this request hasn't been authenticated yet in this thread
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ─── Step 5: Load user from DB ────────────────────────────────────
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // ─── Step 6: Validate token against loaded user ───────────────────
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {

                // ─── Step 7: Create authentication token ──────────────────────
                // This is Spring Security's internal auth object
                // credentials = null (we don't need password after JWT validation)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,        // principal
                                null,               // credentials (null for JWT — already validated)
                                userDetails.getAuthorities()  // roles
                        );

                // Attach request details (IP, session) to auth token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ─── Step 8: Set in SecurityContext ───────────────────────────
                // This tells Spring Security: "this request is authenticated"
                // All subsequent @PreAuthorize checks read from here
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {}, role: {}", username,
                        userDetails.getAuthorities());
            }
        }

        // ─── Step 9: Always continue filter chain ─────────────────────────────
        // Even if auth failed — Spring Security will block at the endpoint level
        filterChain.doFilter(request, response);
    }

    // ─── Extract Bearer token from Authorization header ───────────────────────
    // Header format: "Authorization: Bearer eyJhbGci..."
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // remove "Bearer " prefix (7 chars)
        }

        return null;
    }
}