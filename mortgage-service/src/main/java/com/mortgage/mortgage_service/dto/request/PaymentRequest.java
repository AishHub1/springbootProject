package com.mortgage.mortgage_service.dto.request;

import com.mortgage.mortgage_service.entity.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private BigDecimal principalComponent;

    private BigDecimal interestComponent;

    private LocalDate dueDate;

    private Payment.PaymentMethod paymentMethod;
}