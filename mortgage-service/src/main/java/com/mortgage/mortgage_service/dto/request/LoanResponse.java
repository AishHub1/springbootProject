package com.mortgage.mortgage_service.dto.response;

import com.mortgage.mortgage_service.entity.Loan;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {

    private String loanId;
    private BigDecimal loanAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal interestRate;
    private Loan.LoanType loanType;
    private Loan.LoanStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer termMonths;
    private String customerId;
    private LocalDateTime createdAt;
}