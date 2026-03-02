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
        name = "loans",
        indexes = {
                @Index(name = "idx_loans_customer_id", columnList = "customer_id"),
                @Index(name = "idx_loans_status",      columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "loan_id", updatable = false, nullable = false)
    private String loanId;

    @NotNull(message = "Loan amount is required")
    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "outstanding_balance", precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type")
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "term_months")
    private Integer termMonths;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many loans belong to one customer
    // FetchType.LAZY — customer is NOT loaded unless explicitly accessed (avoids N+1)
    // ForeignKey name — makes the DB constraint identifiable in logs/migrations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_loans_customer_id")
    )
    private Customer customer;

    public enum LoanType {
        FIXED, ARM, FHA, VA, JUMBO
    }

    public enum LoanStatus {
        PENDING, ACTIVE, CLOSED, DEFAULT, FORECLOSURE, DEFAULTED
    }
}