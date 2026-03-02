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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // default read-only — better performance, overridden per write method
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;

    // ─── Existing Methods (minimal changes) ──────────────────────────────────

    @Override
    @Transactional  // write — overrides class-level readOnly
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
    public LoanResponse getLoanById(String loanId) {
        // JOIN FETCH — loads loan + customer in one query instead of two (no N+1)
        Loan loan = loanRepository.findByIdWithCustomer(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        return mapToResponse(loan);
    }

    @Override
    public List<LoanResponse> getLoansByCustomerId(String customerId) {
        return loanRepository.findByCustomer_CustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanResponse> getAllLoans() {
        return loanRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional  // write — overrides class-level readOnly
    public LoanResponse updateLoanStatus(String loanId, String status) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));

        loan.setStatus(Loan.LoanStatus.valueOf(status.toUpperCase()));
        Loan updated = loanRepository.save(loan);
        return mapToResponse(updated);
    }

    @Override
    public BigDecimal getTotalOutstandingBalance(String customerId) {
        BigDecimal total = loanRepository.getTotalOutstandingBalance(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional  // write — overrides class-level readOnly
    public void deleteLoan(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        loanRepository.delete(loan);
        log.info("Loan {} deleted", loanId);
    }

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    @Override
    public Page<LoanResponse> getAllLoansPaginated(Pageable pageable) {
        return loanRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<LoanResponse> getLoansByCustomerIdPaginated(String customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found: " + customerId);
        }
        return loanRepository.findByCustomer_CustomerId(customerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<LoanResponse> getLoansByStatus(Loan.LoanStatus status, Pageable pageable) {
        return loanRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<LoanResponse> getTopLoansByAmount(int n) {
        // Uses Pageable as a LIMIT — PageRequest.of(0, n) = first page of size n
        Pageable topN = PageRequest.of(0, n, Sort.by("loanAmount").descending());
        return loanRepository.findTopLoansByAmount(topN)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getLoanSummaryByStatus() {
        // Raw result: index 0 = LoanStatus, index 1 = count, index 2 = totalOutstanding
        List<Object[]> rows = loanRepository.getLoanSummaryByStatus();

        Map<String, Object> summary = new HashMap<>();
        for (Object[] row : rows) {
            String statusKey = row[0].toString();
            Map<String, Object> stats = new HashMap<>();
            stats.put("count", row[1]);
            stats.put("totalOutstanding", row[2] != null ? row[2] : BigDecimal.ZERO);
            summary.put(statusKey, stats);
        }
        return summary;
    }

    @Override
    @Transactional  // write — bulk UPDATE requires a write transaction
    public int markOverdueLoansAsDefaulted() {
        int updated = loanRepository.markOverdueLoansAsDefaulted(LocalDate.now());
        log.info("Marked {} overdue loans as DEFAULTED", updated);
        return updated;
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

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
                .customerName(loan.getCustomer().getFirstName() + " " + loan.getCustomer().getLastName())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }
}