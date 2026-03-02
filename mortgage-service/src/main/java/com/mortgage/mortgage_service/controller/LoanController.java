package com.mortgage.mortgage_service.controller;

import com.mortgage.mortgage_service.dto.request.LoanRequest;
import com.mortgage.mortgage_service.dto.response.LoanResponse;
import com.mortgage.mortgage_service.entity.Loan;
import com.mortgage.mortgage_service.service.LoanService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Slf4j
public class LoanController {

    private final LoanService loanService;

    // ─── Existing Endpoints (untouched) ──────────────────────────────────────

    // POST /api/v1/loans/customer/{customerId}
    @PostMapping("/customer/{customerId}")
    public ResponseEntity<LoanResponse> createLoan(
            @PathVariable String customerId,
            @Valid @RequestBody LoanRequest request) {
        log.info("REST request to create loan for customer: {}", customerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.createLoan(customerId, request));
    }

    // GET /api/v1/loans/{loanId}
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> getLoanById(@PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getLoanById(loanId));
    }

    // GET /api/v1/loans/customer/{customerId}/balance
    @GetMapping("/customer/{customerId}/balance")
    public ResponseEntity<BigDecimal> getTotalOutstandingBalance(
            @PathVariable String customerId) {
        return ResponseEntity.ok(loanService.getTotalOutstandingBalance(customerId));
    }

    // PATCH /api/v1/loans/{loanId}/status?status=CLOSED
    @PatchMapping("/{loanId}/status")
    public ResponseEntity<LoanResponse> updateLoanStatus(
            @PathVariable String loanId,
            @RequestParam String status) {
        return ResponseEntity.ok(loanService.updateLoanStatus(loanId, status));
    }

    // DELETE /api/v1/loans/{loanId}
    @DeleteMapping("/{loanId}")
    public ResponseEntity<Void> deleteLoan(@PathVariable String loanId) {
        loanService.deleteLoan(loanId);
        return ResponseEntity.noContent().build();
    }

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/loans
     * GET /api/v1/loans?page=0&size=10&sortBy=loanAmount&direction=desc
     *
     * Replaces the old unbounded getAllLoans() — never return all rows without a limit.
     * Falls back to page=0, size=10, sorted by createdAt desc if no params provided.
     */
    @GetMapping
    public ResponseEntity<Page<LoanResponse>> getAllLoans(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(loanService.getAllLoansPaginated(pageable));
    }

    /**
     * GET /api/v1/loans/customer/{customerId}
     * GET /api/v1/loans/customer/{customerId}?page=0&size=5
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<LoanResponse>> getLoansByCustomer(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(loanService.getLoansByCustomerIdPaginated(customerId, pageable));
    }

    /**
     * GET /api/v1/loans/status/ACTIVE
     * GET /api/v1/loans/status/ACTIVE?page=0&size=10
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<LoanResponse>> getLoansByStatus(
            @PathVariable Loan.LoanStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(loanService.getLoansByStatus(status, pageable));
    }

    /**
     * GET /api/v1/loans/top?n=10
     *
     * Returns top N loans by loan amount — default n=5.
     */
    @GetMapping("/top")
    public ResponseEntity<List<LoanResponse>> getTopLoansByAmount(
            @RequestParam(defaultValue = "5") int n) {
        return ResponseEntity.ok(loanService.getTopLoansByAmount(n));
    }

    /**
     * GET /api/v1/loans/summary
     *
     * Returns loan count + total outstanding balance grouped by status.
     * Example response:
     * {
     *   "ACTIVE":  { "count": 42, "totalOutstanding": 8500000.00 },
     *   "PENDING": { "count": 5,  "totalOutstanding": 1200000.00 }
     * }
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getLoanSummaryByStatus() {
        return ResponseEntity.ok(loanService.getLoanSummaryByStatus());
    }

    /**
     * POST /api/v1/loans/admin/mark-defaulted
     *
     * Bulk update — marks all overdue ACTIVE loans as DEFAULTED.
     * Returns count of loans updated.
     * Exposed for manual trigger — on Day 3 this will be secured to ADMIN role only.
     */
    @PostMapping("/admin/mark-defaulted")
    public ResponseEntity<Map<String, Object>> markOverdueLoansAsDefaulted() {
        int updated = loanService.markOverdueLoansAsDefaulted();
        log.info("Admin: marked {} loans as DEFAULTED", updated);
        return ResponseEntity.ok(Map.of(
                "message", "Overdue loans marked as DEFAULTED",
                "updatedCount", updated
        ));
    }
}