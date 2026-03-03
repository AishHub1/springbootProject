package com.mortgage.mortgage_service.service;

import com.mortgage.mortgage_service.dto.request.LoginRequest;
import com.mortgage.mortgage_service.dto.request.RegisterRequest;
import com.mortgage.mortgage_service.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}