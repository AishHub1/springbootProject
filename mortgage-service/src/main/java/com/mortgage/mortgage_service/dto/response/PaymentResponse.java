package com.mortgage.mortgage_service.dto.response;

import com.mortgage.mortgage_service.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private String paymentId;

    // Loan context — avoids a second API call to identify which loan this belongs to
    private String loanId;
    private String customerId;
    private String customerName;      // firstName + lastName — populated in service layer

    private BigDecimal amount;

    // Amortization breakdown — how much of this payment reduces principal vs pays interest
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;

    private LocalDate dueDate;
    private LocalDate paidDate;       // null until payment is COMPLETED

    private Payment.PaymentStatus status;
    private Payment.PaymentMethod paymentMethod;

    // Transaction reference from bank/gateway
    // On Day 4 this will carry the Kafka event message ID
    private String transactionReference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}