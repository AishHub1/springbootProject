package com.mortgage.mortgage_service.service.impl;

import com.mortgage.mortgage_service.dto.request.CustomerRequest;
import com.mortgage.mortgage_service.dto.response.CustomerResponse;
import com.mortgage.mortgage_service.dto.response.LoanResponse;
import com.mortgage.mortgage_service.entity.Customer;
import com.mortgage.mortgage_service.exception.DuplicateResourceException;
import com.mortgage.mortgage_service.exception.ResourceNotFoundException;
import com.mortgage.mortgage_service.repository.CustomerRepository;
import com.mortgage.mortgage_service.repository.LoanRepository;
import com.mortgage.mortgage_service.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // default read-only — overridden per write method
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;  // added — needed for aggregate queries

    // ─── Existing Methods (minimal changes) ──────────────────────────────────

    @Override
    @Transactional  // write — overrides class-level readOnly
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer already exists with email: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .status(Customer.CustomerStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created with id: {}", saved.getCustomerId());
        return mapToResponse(saved);
    }

    @Override
    public CustomerResponse getCustomerById(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return mapToResponse(customer);
    }

    @Override
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));
        return mapToResponse(customer);
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResponse> getActiveCustomers() {
        return customerRepository.findByStatus(Customer.CustomerStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional  // write — overrides class-level readOnly
    public CustomerResponse updateCustomer(String customerId, CustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        // Note: email not updated — business rule

        Customer updated = customerRepository.save(customer);
        return mapToResponse(updated);
    }

    @Override
    @Transactional  // write — overrides class-level readOnly
    public void deleteCustomer(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Soft delete — mark INACTIVE instead of hard delete
        customer.setStatus(Customer.CustomerStatus.INACTIVE);
        customerRepository.save(customer);
        log.info("Customer {} marked as INACTIVE", customerId);
    }

    @Override
    public List<CustomerResponse> searchByName(String name) {
        return customerRepository.findByFirstNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    @Override
    public Page<CustomerResponse> getAllCustomersPaginated(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<CustomerResponse> searchByNamePaginated(String name, Pageable pageable) {
        return customerRepository.searchByName(name, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public CustomerResponse getCustomerWithLoans(String customerId) {
        // JOIN FETCH — loads customer + all loans in a single query (no N+1)
        Customer customer = customerRepository.findByIdWithLoans(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return mapToResponseWithLoans(customer);
    }

    @Override
    public BigDecimal getTotalOutstandingBalance(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        BigDecimal total = customerRepository.getTotalOutstandingBalanceByCustomer(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public List<CustomerResponse> getCustomersWithOverdueLoans() {
        return loanRepository.findCustomersWithOverdueLoans(LocalDate.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResponse> getCustomersWithMoreThanNLoans(int n) {
        return customerRepository.findCustomersWithMoreThanNLoans(n)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countCustomersWithActiveLoans() {
        return customerRepository.countCustomersWithActiveLoans();
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    // Standard mapper — does NOT touch loans collection (avoids N+1 on list endpoints)
    // loanCount and totalOutstandingBalance are populated from aggregate queries
    private CustomerResponse mapToResponse(Customer customer) {
        BigDecimal outstanding = customerRepository
                .getTotalOutstandingBalanceByCustomer(customer.getCustomerId());
        long loanCount = customerRepository
                .getLoanCountByCustomer(customer.getCustomerId());

        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .loanCount((int) loanCount)
                .totalOutstandingBalance(outstanding != null ? outstanding : BigDecimal.ZERO)
                .loans(null)  // null on list endpoints — use getCustomerWithLoans() for full detail
                .build();
    }

    // Detail mapper — only called from getCustomerWithLoans() where loans are JOIN FETCHed
    // Safe to access customer.getLoans() here because they are already loaded in memory
    private CustomerResponse mapToResponseWithLoans(Customer customer) {
        List<LoanResponse> loanResponses = customer.getLoans().stream()
                .map(loan -> LoanResponse.builder()
                        .loanId(loan.getLoanId())
                        .loanAmount(loan.getLoanAmount())
                        .outstandingBalance(loan.getOutstandingBalance())
                        .interestRate(loan.getInterestRate())
                        .loanType(loan.getLoanType())
                        .status(loan.getStatus())
                        .startDate(loan.getStartDate())
                        .endDate(loan.getEndDate())
                        .termMonths(loan.getTermMonths())
                        .customerId(customer.getCustomerId())
                        .customerName(customer.getFirstName() + " " + customer.getLastName())
                        .createdAt(loan.getCreatedAt())
                        .updatedAt(loan.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .loanCount(loanResponses.size())
                .totalOutstandingBalance(
                        customer.getLoans().stream()
                                .filter(l -> l.getStatus() == com.mortgage.mortgage_service.entity.Loan.LoanStatus.ACTIVE)
                                .map(l -> l.getOutstandingBalance() != null ? l.getOutstandingBalance() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                )
                .loans(loanResponses)
                .build();
    }
}