package com.mortgage.mortgage_service.controller;

import com.mortgage.mortgage_service.dto.request.LoanRequest;
import com.mortgage.mortgage_service.dto.response.LoanResponse;
import com.mortgage.mortgage_service.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Slf4j
public class LoanController {

    private final LoanService loanService;

    // POST /api/v1/loans/customer/{customerId}
    @PostMapping("/customer/{customerId}")
    public ResponseEntity<LoanResponse> createLoan(
            @PathVariable String customerId,
            @Valid @RequestBody LoanRequest request) {
        log.info("REST request to create loan for customer: {}", customerId);
        LoanResponse response = loanService.createLoan(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/loans
    @GetMapping
    public ResponseEntity<List<LoanResponse>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    // GET /api/v1/loans/{loanId}
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> getLoanById(
            @PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getLoanById(loanId));
    }

    // GET /api/v1/loans/customer/{customerId}
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoanResponse>> getLoansByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(loanService.getLoansByCustomerId(customerId));
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
    public ResponseEntity<Void> deleteLoan(
            @PathVariable String loanId) {
        loanService.deleteLoan(loanId);
        return ResponseEntity.noContent().build();
    }
}