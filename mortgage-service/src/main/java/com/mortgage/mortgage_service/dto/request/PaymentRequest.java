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
public class PaymentRequest {

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    // Payment method — REQUIRED on submission
    // Accepted values: ACH, WIRE, CHECK, ONLINE, AUTO_DEBIT
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    // Optional — bank/gateway assigns this, not always known at creation time
    // Will be populated when Kafka payment event is received on Day 4
    private String transactionReference;
}