package com.mortgage.mortgage_service.service;

import com.mortgage.mortgage_service.dto.request.LoanRequest;
import com.mortgage.mortgage_service.dto.response.LoanResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LoanService {

    LoanResponse createLoan(String customerId, LoanRequest request);
    LoanResponse getLoanById(String loanId);
    List<LoanResponse> getLoansByCustomerId(String customerId);
    List<LoanResponse> getAllLoans();
    LoanResponse updateLoanStatus(String loanId, String status);
    BigDecimal getTotalOutstandingBalance(String customerId);
    void deleteLoan(String loanId);
}