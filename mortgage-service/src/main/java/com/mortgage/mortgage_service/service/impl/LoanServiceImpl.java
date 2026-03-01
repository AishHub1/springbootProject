package com.mortgage.mortgage_service.service.impl;

import com.mortgage.mortgage_service.dto.request.LoanRequest;
import com.mortgage.mortgage_service.dto.response.LoanResponse;
import com.mortgage.mortgage_service.entity.Customer;
import com.mortgage.mortgage_service.entity.Loan;
import com.mortgage.mortgage_service.exception.ResourceNotFoundException;
import com.mortgage.mortgage_service.repository.CustomerRepository;
import com.mortgage.mortgage_service.repository.LoanRepository;
import com.mortgage.mortgage_service.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;

    @Override
    public LoanResponse createLoan(String customerId, LoanRequest request) {
        log.info("Creating loan for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        Loan loan = Loan.builder()
                .loanAmount(request.getLoanAmount())
                .outstandingBalance(request.getLoanAmount()) // initially full amount is outstanding
                .interestRate(request.getInterestRate())
                .loanType(Loan.LoanType.valueOf(request.getLoanType()))
                .termMonths(request.getTermMonths())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .endDate(request.getStartDate() != null
                        ? request.getStartDate().plusMonths(request.getTermMonths())
                        : LocalDate.now().plusMonths(request.getTermMonths()))
                .status(Loan.LoanStatus.ACTIVE)
                .customer(customer)
                .build();

        Loan saved = loanRepository.save(loan);
        log.info("Loan created with id: {}", saved.getLoanId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoanById(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        return mapToResponse(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getLoansByCustomerId(String customerId) {
        return loanRepository.findByCustomer_CustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> getAllLoans() {
        return loanRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LoanResponse updateLoanStatus(String loanId, String status) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));

        loan.setStatus(Loan.LoanStatus.valueOf(status.toUpperCase()));
        Loan updated = loanRepository.save(loan);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingBalance(String customerId) {
        BigDecimal total = loanRepository.getTotalOutstandingBalance(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public void deleteLoan(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        loanRepository.delete(loan);
        log.info("Loan {} deleted", loanId);
    }

    // ── Mapper ───────────────────────────────────────────────────────
    private LoanResponse mapToResponse(Loan loan) {
        return LoanResponse.builder()
                .loanId(loan.getLoanId())
                .loanAmount(loan.getLoanAmount())
                .outstandingBalance(loan.getOutstandingBalance())
                .interestRate(loan.getInterestRate())
                .loanType(loan.getLoanType())
                .status(loan.getStatus())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .termMonths(loan.getTermMonths())
                .customerId(loan.getCustomer().getCustomerId())
                .createdAt(loan.getCreatedAt())
                .build();
    }
}