package com.mortgage.mortgage_service.service;

import com.mortgage.mortgage_service.dto.request.CustomerRequest;
import com.mortgage.mortgage_service.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerService {

    // ─── Existing Methods (untouched) ────────────────────────────────────────

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(String customerId);

    CustomerResponse getCustomerByEmail(String email);

    List<CustomerResponse> getAllCustomers();

    List<CustomerResponse> getActiveCustomers();

    CustomerResponse updateCustomer(String customerId, CustomerRequest request);

    void deleteCustomer(String customerId);

    List<CustomerResponse> searchByName(String name);

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    // Paginated versions — use these in controllers instead of the list versions
    Page<CustomerResponse> getAllCustomersPaginated(Pageable pageable);

    Page<CustomerResponse> searchByNamePaginated(String name, Pageable pageable);

    // Loads customer + all loans in a single JOIN FETCH query
    // Use on the customer detail page where loans must be displayed
    CustomerResponse getCustomerWithLoans(String customerId);

    // Total outstanding balance across all ACTIVE loans for a customer
    BigDecimal getTotalOutstandingBalance(String customerId);

    // Customers whose ACTIVE loans have passed their end date
    List<CustomerResponse> getCustomersWithOverdueLoans();

    // Customers with more than N loans — risk/exposure monitoring
    List<CustomerResponse> getCustomersWithMoreThanNLoans(int n);

    // Dashboard metric — how many customers have at least one ACTIVE loan
    long countCustomersWithActiveLoans();
}