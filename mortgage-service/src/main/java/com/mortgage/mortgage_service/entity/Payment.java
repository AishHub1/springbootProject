package com.mortgage.mortgage_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_loan_id",  columnList = "loan_id"),
                @Index(name = "idx_payments_status",   columnList = "status"),
                @Index(name = "idx_payments_due_date", columnList = "due_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private String paymentId;

    // Many payments belong to one loan
    // LAZY — loan details only loaded when explicitly accessed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "loan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_loan_id")
    )
    private Loan loan;

    @NotNull(message = "Payment amount is required")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // How much of this payment goes toward principal vs interest
    @Column(name = "principal_component", precision = 15, scale = 2)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", precision = 15, scale = 2)
    private BigDecimal interestComponent;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    // Reference number from bank/payment gateway — useful for Kafka events on Day 4
    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING,    // scheduled but not yet due
        DUE,        // due date reached, awaiting payment
        COMPLETED,  // successfully paid
        FAILED,     // payment attempt failed
        OVERDUE,    // past due date, not paid
        WAIVED      // manually waived by servicer
    }

    public enum PaymentMethod {
        ACH,            // bank transfer
        WIRE,           // wire transfer
        CHECK,          // physical check
        ONLINE,         // online portal
        AUTO_DEBIT      // automatic monthly debit
    }
}