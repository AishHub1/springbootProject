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

    // ─── Existing (untouched) ─────────────────────────────────────────────────

    List<Payment> findByLoan_LoanId(String loanId);
    Page<Payment> findByLoan_LoanId(String loanId, Pageable pageable);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);
    List<Payment> findByDueDate(LocalDate dueDate);
    List<Payment> findByDueDateBetween(LocalDate from, LocalDate to);
    Optional<Payment> findByTransactionReference(String transactionReference);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.loan l
            WHERE p.paymentId = :paymentId
            """)
    Optional<Payment> findByIdWithLoan(@Param("paymentId") String paymentId);

    @Query("""
            SELECT SUM(p.amount) FROM Payment p
            WHERE p.loan.loanId = :loanId
              AND p.status = 'COMPLETED'
            """)
    BigDecimal getTotalPaidByLoan(@Param("loanId") String loanId);

    @Query("""
            SELECT SUM(p.amount) FROM Payment p
            WHERE p.loan.customer.customerId = :customerId
              AND p.status = 'COMPLETED'
            """)
    BigDecimal getTotalPaidByCustomer(@Param("customerId") String customerId);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.status IN ('DUE', 'PENDING')
              AND p.dueDate < :today
            """)
    List<Payment> findOverduePayments(@Param("today") LocalDate today);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.loan.loanId = :loanId
              AND p.status IN ('DUE', 'PENDING')
              AND p.dueDate < :today
            """)
    List<Payment> findOverduePaymentsByLoan(@Param("loanId") String loanId,
                                             @Param("today") LocalDate today);

    @Query("""
            SELECT p.status, COUNT(p) FROM Payment p
            GROUP BY p.status
            """)
    List<Object[]> countPaymentsByStatus();

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

    @Modifying
    @Query("""
            UPDATE Payment p SET p.status = 'OVERDUE'
            WHERE p.status IN ('DUE', 'PENDING')
              AND p.dueDate < :today
            """)
    int markPaymentsAsOverdue(@Param("today") LocalDate today);

    // ─── NEW: Added for PaymentServiceImpl ───────────────────────────────────

    // Count by status — used in getPaymentSummary()
    long countByStatus(Payment.PaymentStatus status);

    // Payment history sorted chronologically — used in getPaymentHistory()
    // Spring derives: WHERE loan.loanId = ? ORDER BY paidDate ASC
    List<Payment> findByLoan_LoanIdOrderByPaidDateAsc(String loanId);

    // Sum of all payments by status — used in getPaymentSummary() for totalCollected
    @Query("""
            SELECT SUM(p.amount) FROM Payment p
            WHERE p.status = :status
            """)
    BigDecimal sumAmountByStatus(@Param("status") Payment.PaymentStatus status);
}