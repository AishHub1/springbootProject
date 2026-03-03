package com.mortgage.mortgage_service.controller;

import com.mortgage.mortgage_service.dto.request.PaymentRequest;
import com.mortgage.mortgage_service.dto.response.PaymentResponse;
import com.mortgage.mortgage_service.service.PaymentService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // ─── ADMIN ONLY ───────────────────────────────────────────────────────────

    // GET /api/v1/payments — all payments paginated, ADMIN only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "0")           int page,
            @RequestParam(defaultValue = "10")          int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc")        String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(paymentService.getAllPaymentsPaginated(pageable));
    }

    // GET /api/v1/payments/summary — ADMIN only (dashboard)
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPaymentSummary() {
        return ResponseEntity.ok(paymentService.getPaymentSummary());
    }

    // DELETE /api/v1/payments/{paymentId} — ADMIN only
    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable String paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }

    // ─── CUSTOMER + ADMIN ─────────────────────────────────────────────────────

    // POST /api/v1/payments/loan/{loanId}
    // CUSTOMER makes payment on their own loan
    // ADMIN can make payment on any loan
    @PostMapping("/loan/{loanId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> makePayment(
            @PathVariable String loanId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} making payment for loan {}", userDetails.getUsername(), loanId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.makePayment(loanId, request));
    }

    // GET /api/v1/payments/{paymentId}
    // ADMIN sees any payment, CUSTOMER sees own
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable String paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} requesting payment {}", userDetails.getUsername(), paymentId);
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    // GET /api/v1/payments/loan/{loanId}
    // ADMIN sees payments for any loan
    // CUSTOMER sees payments only for their own loan — ownership checked in service
    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByLoan(
            @PathVariable String loanId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User {} requesting payments for loan {}", userDetails.getUsername(), loanId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("paymentDate").descending());
        return ResponseEntity.ok(paymentService.getPaymentsByLoanPaginated(loanId, pageable));
    }

    // GET /api/v1/payments/loan/{loanId}/history
    // Full payment history for a loan — sorted chronologically
    @GetMapping("/loan/{loanId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @PathVariable String loanId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("User {} requesting payment history for loan {}",
                userDetails.getUsername(), loanId);
        return ResponseEntity.ok(paymentService.getPaymentHistory(loanId));
    }
}