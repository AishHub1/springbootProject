package com.mortgage.mortgage_service.service;

import com.mortgage.mortgage_service.dto.request.LoanRequest;
import com.mortgage.mortgage_service.dto.response.LoanResponse;
import com.mortgage.mortgage_service.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface LoanService {

    // ─── Existing Methods (untouched) ────────────────────────────────────────

    LoanResponse createLoan(String customerId, LoanRequest request);

    LoanResponse getLoanById(String loanId);

    List<LoanResponse> getLoansByCustomerId(String customerId);

    List<LoanResponse> getAllLoans();

    LoanResponse updateLoanStatus(String loanId, String status);

    BigDecimal getTotalOutstandingBalance(String customerId);

    void deleteLoan(String loanId);

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    // Paginated versions — use these in controllers instead of the list versions
    Page<LoanResponse> getAllLoansPaginated(Pageable pageable);

    Page<LoanResponse> getLoansByCustomerIdPaginated(String customerId, Pageable pageable);

    Page<LoanResponse> getLoansByStatus(Loan.LoanStatus status, Pageable pageable);

    // Top N loans by amount — pass N from controller
    // e.g. getTopLoansByAmount(10) returns the 10 largest loans
    List<LoanResponse> getTopLoansByAmount(int n);

    // Loan count + total outstanding grouped by status
    // Map key = status name (e.g. "ACTIVE"), value = [count, totalOutstanding]
    Map<String, Object> getLoanSummaryByStatus();

    // Bulk update — marks all overdue ACTIVE loans as DEFAULTED
    // Returns number of loans updated
    int markOverdueLoansAsDefaulted();
}