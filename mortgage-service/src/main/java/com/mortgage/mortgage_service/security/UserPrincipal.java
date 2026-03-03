package com.mortgage.mortgage_service.security;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// UserPrincipal is the bridge between YOUR User entity and Spring Security
// Spring Security doesn't know about your Customer/User entity directly
// It works with UserDetails interface — so we wrap our user in this class

@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final String username;   // email in our case
    private final String password;   // BCrypt hashed
    private final String role;       // "ROLE_ADMIN" or "ROLE_CUSTOMER"

    // ─── 1. AUTHORITIES ────────────────────────────────────────────────────────
    // Spring Security reads permissions from here
    // SimpleGrantedAuthority wraps a role string like "ROLE_ADMIN"
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    // ─── 2. ACCOUNT STATUS FLAGS ───────────────────────────────────────────────
    // For now all true — in prod you'd check DB flags like isActive, isVerified
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}