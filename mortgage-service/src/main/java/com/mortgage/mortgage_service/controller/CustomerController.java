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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    // ─── Existing Endpoints (untouched) ──────────────────────────────────────

    // POST /api/v1/customers
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        log.info("REST request to create customer: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(request));
    }

    // GET /api/v1/customers/{customerId}
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    // GET /api/v1/customers/email/{email}
    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(
            @PathVariable String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }

    // GET /api/v1/customers/active
    @GetMapping("/active")
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {
        return ResponseEntity.ok(customerService.getActiveCustomers());
    }

    // PUT /api/v1/customers/{customerId}
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
    }

    // DELETE /api/v1/customers/{customerId}
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/customers
     * GET /api/v1/customers?page=0&size=10&sortBy=lastName&direction=asc
     *
     * Replaces the old unbounded getAllCustomers() list.
     */
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "10")       int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")     String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(customerService.getAllCustomersPaginated(pageable));
    }

    /**
     * GET /api/v1/customers/search?name=john
     * GET /api/v1/customers/search?name=john&page=0&size=5
     *
     * Searches first name OR last name, case-insensitive, paginated.
     * Replaces the old unbounded searchByName() list.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        return ResponseEntity.ok(customerService.searchByNamePaginated(name, pageable));
    }

    /**
     * GET /api/v1/customers/{customerId}/loans
     *
     * Returns customer + full loan list in a single JOIN FETCH query.
     * Use this endpoint when you need to display a customer's loans.
     * GET /customers/{id} does NOT include loans — this one does.
     */
    @GetMapping("/{customerId}/loans")
    public ResponseEntity<CustomerResponse> getCustomerWithLoans(
            @PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerWithLoans(customerId));
    }

    /**
     * GET /api/v1/customers/overdue
     *
     * Returns all customers who have at least one ACTIVE loan past its end date.
     * Used by collections team dashboard.
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<CustomerResponse>> getCustomersWithOverdueLoans() {
        return ResponseEntity.ok(customerService.getCustomersWithOverdueLoans());
    }

    /**
     * GET /api/v1/customers/high-exposure?minLoans=3
     *
     * Returns customers with more than N loans — risk/exposure monitoring.
     * Default minLoans=2 if not specified.
     */
    @GetMapping("/high-exposure")
    public ResponseEntity<List<CustomerResponse>> getCustomersWithMoreThanNLoans(
            @RequestParam(defaultValue = "2") int minLoans) {
        return ResponseEntity.ok(customerService.getCustomersWithMoreThanNLoans(minLoans));
    }

    /**
     * GET /api/v1/customers/stats
     *
     * Returns dashboard metrics — currently active loan customer count.
     * Easy to extend with more stats later.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCustomerStats() {
        long activeCount = customerService.countCustomersWithActiveLoans();
        return ResponseEntity.ok(Map.of(
                "customersWithActiveLoans", activeCount
        ));
    }
}