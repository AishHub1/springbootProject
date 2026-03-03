package com.mortgage.mortgage_service.service.impl;

import com.mortgage.mortgage_service.dto.request.LoginRequest;
import com.mortgage.mortgage_service.dto.request.RegisterRequest;
import com.mortgage.mortgage_service.dto.response.AuthResponse;
import com.mortgage.mortgage_service.entity.Customer;
import com.mortgage.mortgage_service.exception.DuplicateResourceException;
import com.mortgage.mortgage_service.repository.CustomerRepository;
import com.mortgage.mortgage_service.security.JwtUtil;
import com.mortgage.mortgage_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ─── REGISTER ──────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 1. Check duplicate email
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Customer already exists with email: " + request.getEmail()
            );
        }

        // 2. Build Customer entity
        // Password is BCrypt hashed — NEVER store plain text
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt here
                .phone(request.getPhone())
                .address(request.getAddress())
                .role("ROLE_CUSTOMER")   // default role on registration
                .status(Customer.CustomerStatus.ACTIVE)
                .build();

        customerRepository.save(customer);
        log.info("New customer registered: {}", customer.getEmail());

        // 3. Generate JWT immediately after register — user is logged in right away
        String token = jwtUtil.generateToken(customer.getEmail(), customer.getRole());

        return AuthResponse.of(token, customer.getEmail(), customer.getRole(), jwtExpiration);
    }

    // ─── LOGIN ─────────────────────────────────────────────────────────────────
    @Override
    public AuthResponse login(LoginRequest request) {

        // 1. AuthenticationManager does two things internally:
        //    a) calls loadUserByUsername(email) → loads user from DB
        //    b) calls BCrypt.matches(rawPassword, hashedPassword) → verifies password
        //    throws BadCredentialsException if either fails → caught by GlobalExceptionHandler
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Get authenticated user details
        // getPrincipal() returns the UserDetails object set during authentication
        org.springframework.security.core.userdetails.UserDetails userDetails =
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();

        // 3. Extract role from authorities
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("ROLE_CUSTOMER");

        // 4. Generate JWT
        String token = jwtUtil.generateToken(userDetails.getUsername(), role);
        log.info("Customer logged in: {}", userDetails.getUsername());

        return AuthResponse.of(token, userDetails.getUsername(), role, jwtExpiration);
    }
}