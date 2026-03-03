package com.mortgage.mortgage_service.controller;

import com.mortgage.mortgage_service.dto.request.CustomerRequest;
import com.mortgage.mortgage_service.dto.response.CustomerResponse;
import com.mortgage.mortgage_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    // ─── ADMIN ONLY ───────────────────────────────────────────────────────────

    // POST /api/v1/customers — only ADMIN can create customers directly
    // (customers self-register via /auth/register)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        log.info("REST request to create customer: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(request));
    }

    // GET /api/v1/customers — paginated list, ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(customerService.getAllCustomersPaginated(pageable));
    }

    // GET /api/v1/customers/{customerId} — ADMIN only
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    // GET /api/v1/customers/email/{email} — ADMIN only
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(
            @PathVariable String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }

    // GET /api/v1/customers/active — ADMIN only
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {
        return ResponseEntity.ok(customerService.getActiveCustomers());
    }

    // GET /api/v1/customers/search?name=john — ADMIN only
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        return ResponseEntity.ok(customerService.searchByNamePaginated(name, pageable));
    }

    // GET /api/v1/customers/overdue — ADMIN only (collections team)
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getCustomersWithOverdueLoans() {
        return ResponseEntity.ok(customerService.getCustomersWithOverdueLoans());
    }

    // GET /api/v1/customers/high-exposure — ADMIN only (risk team)
    @GetMapping("/high-exposure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getCustomersWithMoreThanNLoans(
            @RequestParam(defaultValue = "2") int minLoans) {
        return ResponseEntity.ok(customerService.getCustomersWithMoreThanNLoans(minLoans));
    }

    // GET /api/v1/customers/stats — ADMIN only (dashboard)
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCustomerStats() {
        long activeCount = customerService.countCustomersWithActiveLoans();
        return ResponseEntity.ok(Map.of("customersWithActiveLoans", activeCount));
    }

    // PUT /api/v1/customers/{customerId} — ADMIN only
    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    // DELETE /api/v1/customers/{customerId} — ADMIN only
    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    // ─── CUSTOMER — OWN DATA ONLY ─────────────────────────────────────────────

    // GET /api/v1/customers/{customerId}/loans
    // ADMIN can see any customer's loans
    // CUSTOMER can only see their OWN loans
    // #customerId == authentication.name checks if path variable matches logged-in email
    @GetMapping("/{customerId}/loans")
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")
    public ResponseEntity<CustomerResponse> getCustomerWithLoans(
            @PathVariable String customerId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} requesting loans for customer {}", userDetails.getUsername(), customerId);
        return ResponseEntity.ok(customerService.getCustomerWithLoans(customerId));
    }
}