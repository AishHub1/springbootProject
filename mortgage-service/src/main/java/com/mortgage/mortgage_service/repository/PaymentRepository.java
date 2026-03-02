package com.mortgage.mortgage_service.repository;

import com.mortgage.mortgage_service.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // ─── Derived Queries ─────────────────────────────────────────────────────

    // All payments for a specific loan
    List<Payment> findByLoan_LoanId(String loanId);

    // Paginated — use this in controllers
    Page<Payment> findByLoan_LoanId(String loanId, Pageable pageable);

    // Payments by status
    List<Payment> findByStatus(Payment.PaymentStatus status);

    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);

    // Payments due on a specific date
    List<Payment> findByDueDate(LocalDate dueDate);

    // Payments due between two dates — useful for upcoming payment schedules
    List<Payment> findByDueDateBetween(LocalDate from, LocalDate to);

    // Lookup by transaction reference — used when Kafka event comes back with a ref
    Optional<Payment> findByTransactionReference(String transactionReference);

    // ─── JPQL Queries ────────────────────────────────────────────────────────

    // All payments for a loan with their loan details loaded (avoids N+1)
    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.loan l
            WHERE p.paymentId = :paymentId
            """)
    Optional<Payment> findByIdWithLoan(@Param("paymentId") String paymentId);

    // Total amount paid (COMPLETED only) for a given loan
    @Query("""
            SELECT SUM(p.amount) FROM Payment p
            WHERE p.loan.loanId = :loanId
              AND p.status = 'COMPLETED'
            """)
    BigDecimal getTotalPaidByLoan(@Param("loanId") String loanId);

    // Total amount paid across all loans for a customer
    @Query("""
            SELECT SUM(p.amount) FROM Payment p
            WHERE p.loan.customer.customerId = :customerId
              AND p.status = 'COMPLETED'
            """)
    BigDecimal getTotalPaidByCustomer(@Param("customerId") String customerId);

    // Overdue payments — DUE or PENDING payments past their due date
    @Query("""
            SELECT p FROM Payment p
            WHERE p.status IN ('DUE', 'PENDING')
              AND p.dueDate < :today
            """)
    List<Payment> findOverduePayments(@Param("today") LocalDate today);

    // Overdue payments for a specific loan
    @Query("""
            SELECT p FROM Payment p
            WHERE p.loan.loanId = :loanId
              AND p.status IN ('DUE', 'PENDING')
              AND p.dueDate < :today
            """)
    List<Payment> findOverduePaymentsByLoan(@Param("loanId") String loanId,
                                             @Param("today") LocalDate today);

    // Payment count grouped by status — for dashboard summary
    @Query("""
            SELECT p.status, COUNT(p) FROM Payment p
            GROUP BY p.status
            """)
    List<Object[]> countPaymentsByStatus();

    // Monthly payment summary — total collected per month
    // Prep for Kafka Day 4 — this same data will flow through payment events
    @Query(value = """
            SELECT
                EXTRACT(YEAR  FROM paid_date) AS year,
                EXTRACT(MONTH FROM paid_date) AS month,
                COUNT(*)                       AS payment_count,
                SUM(amount)                    AS total_collected
            FROM payments
            WHERE status = 'COMPLETED'
              AND paid_date IS NOT NULL
            GROUP BY year, month
            ORDER BY year, month
            """, nativeQuery = true)
    List<Object[]> getMonthlyPaymentSummary();

    // ─── Bulk Updates ────────────────────────────────────────────────────────

    // Mark all PENDING/DUE payments as OVERDUE if past due date
    // Call this from a @Scheduled job daily
    // @Transactional must be on the calling service method
    @Modifying
    @Query("""
            UPDATE Payment p SET p.status = 'OVERDUE'
            WHERE p.status IN ('DUE', 'PENDING')
              AND p.dueDate < :today
            """)
    int markPaymentsAsOverdue(@Param("today") LocalDate today);
}