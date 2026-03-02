package com.mortgage.mortgage_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequest {

    private String customerId;

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1,000")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount is 10,000,000")
    private BigDecimal loanAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.01", message = "Interest rate must be at least 0.01%")
    @DecimalMax(value = "30.00", message = "Interest rate cannot exceed 30%")
    private BigDecimal interestRate;

    // Kept as String to match your existing design — validated against enum values in service layer
    @NotNull(message = "Loan type is required")
    private String loanType;  // FIXED, ARM, FHA, VA, JUMBO

    @NotNull(message = "Term is required")
    @Min(value = 12,  message = "Minimum term is 12 months")
    @Max(value = 480, message = "Maximum term is 480 months (40 years)")
    private Integer termMonths;

    // FutureOrPresent — start date cannot be in the past
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;
}