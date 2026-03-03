package com.mortgage.mortgage_service.service.impl;

import com.mortgage.mortgage_service.dto.request.PaymentRequest;
import com.mortgage.mortgage_service.dto.response.PaymentResponse;
import com.mortgage.mortgage_service.entity.Loan;
import com.mortgage.mortgage_service.entity.Payment;
import com.mortgage.mortgage_service.exception.ResourceNotFoundException;
import com.mortgage.mortgage_service.repository.LoanRepository;
import com.mortgage.mortgage_service.repository.PaymentRepository;
import com.mortgage.mortgage_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;

    // ─── MAKE PAYMENT ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PaymentResponse makePayment(String loanId, PaymentRequest request) {

        // 1. Load loan — throws 404 if not found
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan not found with id: " + loanId));

        // 2. Guard — can't pay a closed/defaulted loan
        if (loan.getStatus() == Loan.LoanStatus.CLOSED ||
            loan.getStatus() == Loan.LoanStatus.DEFAULTED) {
            throw new IllegalStateException(
                    "Cannot make payment on a " + loan.getStatus() + " loan");
        }

        // 3. Build payment entity
        Payment payment = Payment.builder()
                .loan(loan)
                .amount(request.getAmount())
                .principalComponent(request.getPrincipalComponent())
                .interestComponent(request.getInterestComponent())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now())
                .paidDate(LocalDate.now())
                .status(Payment.PaymentStatus.COMPLETED)
                .paymentMethod(request.getPaymentMethod())
                .transactionReference(UUID.randomUUID().toString())  // generate unique ref
                .build();

        Payment saved = paymentRepository.save(payment);

        // 4. Reduce outstanding balance on loan
        BigDecimal newBalance = loan.getOutstandingBalance()
                .subtract(request.getAmount());
        loan.setOutstandingBalance(newBalance.max(BigDecimal.ZERO)); // floor at 0

        // 5. Auto-close loan if fully paid
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(Loan.LoanStatus.CLOSED);
            log.info("Loan {} fully paid and closed", loanId);
        }

        loanRepository.save(loan);
        log.info("Payment {} recorded for loan {}", saved.getPaymentId(), loanId);

        return mapToResponse(saved);
    }

    // ─── GET PAYMENT BY ID ─────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId));
        return mapToResponse(payment);
    }

    // ─── GET PAYMENTS BY LOAN (paginated) ─────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByLoanPaginated(String loanId, Pageable pageable) {
        // verify loan exists first
        if (!loanRepository.existsById(loanId)) {
            throw new ResourceNotFoundException("Loan not found with id: " + loanId);
        }
        return paymentRepository.findByLoan_LoanId(loanId, pageable)
                .map(this::mapToResponse);
    }

    // ─── GET FULL PAYMENT HISTORY (sorted chronologically) ────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory(String loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new ResourceNotFoundException("Loan not found with id: " + loanId);
        }
        return paymentRepository.findByLoan_LoanIdOrderByPaidDateAsc(loanId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── GET ALL PAYMENTS PAGINATED (ADMIN) ───────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPaymentsPaginated(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    // ─── PAYMENT SUMMARY (ADMIN dashboard) ────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentSummary() {
        long totalPayments = paymentRepository.count();
        long completedPayments = paymentRepository.countByStatus(Payment.PaymentStatus.COMPLETED);
        long overduePayments = paymentRepository.countByStatus(Payment.PaymentStatus.OVERDUE);
        long pendingPayments = paymentRepository.countByStatus(Payment.PaymentStatus.PENDING);

        BigDecimal totalCollected = paymentRepository.sumAmountByStatus(
                Payment.PaymentStatus.COMPLETED);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPayments", totalPayments);
        summary.put("completedPayments", completedPayments);
        summary.put("overduePayments", overduePayments);
        summary.put("pendingPayments", pendingPayments);
        summary.put("totalCollected", totalCollected != null ? totalCollected : BigDecimal.ZERO);
        return summary;
    }

    // ─── DELETE PAYMENT (ADMIN) ────────────────────────────────────────────────
    @Override
    @Transactional
    public void deletePayment(String paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new ResourceNotFoundException("Payment not found with id: " + paymentId);
        }
        paymentRepository.deleteById(paymentId);
        log.info("Payment {} deleted by admin", paymentId);
    }

    // ─── MAPPER ───────────────────────────────────────────────────────────────
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .loanId(payment.getLoan().getLoanId())
                .amount(payment.getAmount())
                .principalComponent(payment.getPrincipalComponent())
                .interestComponent(payment.getInterestComponent())
                .dueDate(payment.getDueDate())
                .paidDate(payment.getPaidDate())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}