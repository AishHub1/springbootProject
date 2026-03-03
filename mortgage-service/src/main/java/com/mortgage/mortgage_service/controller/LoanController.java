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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    // ─── ADMIN ONLY ───────────────────────────────────────────────────────────

    // POST /api/v1/loans/customer/{customerId} — only ADMIN creates loans
    @PostMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> createLoan(
            @PathVariable String customerId,
            @Valid @RequestBody LoanRequest request) {
        log.info("REST request to create loan for customer: {}", customerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.createLoan(customerId, request));
    }

    // GET /api/v1/loans — all loans paginated, ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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

    // GET /api/v1/loans/status/{status} — ADMIN only
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<LoanResponse>> getLoansByStatus(
            @PathVariable Loan.LoanStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(loanService.getLoansByStatus(status, pageable));
    }

    // GET /api/v1/loans/top?n=5 — ADMIN only
    @GetMapping("/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponse>> getTopLoansByAmount(
            @RequestParam(defaultValue = "5") int n) {
        return ResponseEntity.ok(loanService.getTopLoansByAmount(n));
    }

    // GET /api/v1/loans/summary — ADMIN only (dashboard)
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getLoanSummaryByStatus() {
        return ResponseEntity.ok(loanService.getLoanSummaryByStatus());
    }

    // PATCH /api/v1/loans/{loanId}/status — ADMIN only
    @PatchMapping("/{loanId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> updateLoanStatus(
            @PathVariable String loanId,
            @RequestParam String status) {
        return ResponseEntity.ok(loanService.updateLoanStatus(loanId, status));
    }

    // DELETE /api/v1/loans/{loanId} — ADMIN only
    @DeleteMapping("/{loanId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLoan(@PathVariable String loanId) {
        loanService.deleteLoan(loanId);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/loans/admin/mark-defaulted — ADMIN only
    @PostMapping("/admin/mark-defaulted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> markOverdueLoansAsDefaulted() {
        int updated = loanService.markOverdueLoansAsDefaulted();
        log.info("Admin: marked {} loans as DEFAULTED", updated);
        return ResponseEntity.ok(Map.of(
                "message", "Overdue loans marked as DEFAULTED",
                "updatedCount", updated
        ));
    }

    // ─── CUSTOMER + ADMIN ─────────────────────────────────────────────────────

    // GET /api/v1/loans/{loanId}
    // ADMIN sees any loan
    // CUSTOMER — in real app you'd verify ownership in service layer
    @GetMapping("/{loanId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<LoanResponse> getLoanById(
            @PathVariable String loanId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} requesting loan {}", userDetails.getUsername(), loanId);
        return ResponseEntity.ok(loanService.getLoanById(loanId));
    }

    // GET /api/v1/loans/customer/{customerId}
    // ADMIN sees any customer's loans
    // CUSTOMER sees only their own — SpEL checks email matches
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")
    public ResponseEntity<Page<LoanResponse>> getLoansByCustomer(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User {} requesting loans for customer {}",
                userDetails.getUsername(), customerId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(loanService.getLoansByCustomerIdPaginated(customerId, pageable));
    }

    // GET /api/v1/loans/customer/{customerId}/balance
    // ADMIN sees any, CUSTOMER sees own balance only
    @GetMapping("/customer/{customerId}/balance")
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")
    public ResponseEntity<BigDecimal> getTotalOutstandingBalance(
            @PathVariable String customerId) {
        return ResponseEntity.ok(loanService.getTotalOutstandingBalance(customerId));
    }
}