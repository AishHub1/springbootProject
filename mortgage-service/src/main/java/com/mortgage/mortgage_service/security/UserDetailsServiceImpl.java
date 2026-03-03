package com.mortgage.mortgage_service.security;

import com.mortgage.mortgage_service.repository.CustomerRepository;
import com.mortgage.mortgage_service.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security calls loadUserByUsername() automatically during authentication
// This is the bridge between Spring Security and your database

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;

    // ─── Spring Security calls this during login ────────────────────────────────
    // "username" here is actually the email in our system
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Customer customer = customerRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username
                ));

        // Wrap Customer entity into UserPrincipal (UserDetails implementation)
        return new UserPrincipal(
                customer.getEmail(),
                customer.getPassword(),
                customer.getRole()   // "ROLE_ADMIN" or "ROLE_CUSTOMER"
        );
    }
}