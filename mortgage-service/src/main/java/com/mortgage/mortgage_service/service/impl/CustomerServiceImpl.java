package com.mortgage.mortgage_service.service.impl;

import com.mortgage.mortgage_service.dto.request.CustomerRequest;
import com.mortgage.mortgage_service.dto.response.CustomerResponse;
import com.mortgage.mortgage_service.dto.response.LoanResponse;
import com.mortgage.mortgage_service.entity.Customer;
import com.mortgage.mortgage_service.exception.DuplicateResourceException;
import com.mortgage.mortgage_service.exception.ResourceNotFoundException;
import com.mortgage.mortgage_service.repository.CustomerRepository;
import com.mortgage.mortgage_service.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service                    // marks this as a Spring Bean
@RequiredArgsConstructor    // Lombok: generates constructor injection
@Slf4j                      // Lombok: gives us log.info(), log.error() etc
@Transactional              // every method runs in a transaction by default
public class CustomerServiceImpl implements CustomerService {

    // Constructor injection — best practice over @Autowired on field
    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        // Business rule — no duplicate emails
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer already exists with email: " + request.getEmail());
        }

        // Map request DTO → Entity
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

        // Map Entity → Response DTO
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)  // optimization for read operations
    public CustomerResponse getCustomerById(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));
        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getActiveCustomers() {
        return customerRepository.findByStatus(Customer.CustomerStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponse updateCustomer(String customerId, CustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Update fields
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        // Note: we don't update email — business rule!

        Customer updated = customerRepository.save(customer);
        return mapToResponse(updated);
    }

    @Override
    public void deleteCustomer(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Soft approach — mark inactive instead of hard delete
        customer.setStatus(Customer.CustomerStatus.INACTIVE);
        customerRepository.save(customer);
        log.info("Customer {} marked as INACTIVE", customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchByName(String name) {
        return customerRepository.findByFirstNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Mapper (Entity → Response DTO) ──────────────────────────────
    private CustomerResponse mapToResponse(Customer customer) {
        List<LoanResponse> loanResponses = customer.getLoans() == null ? List.of() :
                customer.getLoans().stream()
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
                                .createdAt(loan.getCreatedAt())
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
                .loans(loanResponses)
                .build();
    }
}