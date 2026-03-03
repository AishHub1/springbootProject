package com.mortgage.mortgage_service.service;

import com.mortgage.mortgage_service.dto.request.PaymentRequest;
import com.mortgage.mortgage_service.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    // CUSTOMER + ADMIN
    PaymentResponse makePayment(String loanId, PaymentRequest request);
    PaymentResponse getPaymentById(String paymentId);
    Page<PaymentResponse> getPaymentsByLoanPaginated(String loanId, Pageable pageable);
    List<PaymentResponse> getPaymentHistory(String loanId);

    // ADMIN only
    Page<PaymentResponse> getAllPaymentsPaginated(Pageable pageable);
    Map<String, Object> getPaymentSummary();
    void deletePayment(String paymentId);
}