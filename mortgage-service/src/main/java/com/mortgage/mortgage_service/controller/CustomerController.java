package com.mortgage.mortgage_service.controller;

import com.mortgage.mortgage_service.dto.request.CustomerRequest;
import com.mortgage.mortgage_service.dto.response.CustomerResponse;
import com.mortgage.mortgage_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    // POST /api/v1/customers
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        log.info("REST request to create customer: {}", request.getEmail());
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/customers
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
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

    // GET /api/v1/customers/search?name=john
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> searchByName(
            @RequestParam String name) {
        return ResponseEntity.ok(customerService.searchByName(name));
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
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}