package com.mortgage.mortgage_service.repository;

import com.mortgage.mortgage_service.entity.Loan;
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
public interface LoanRepository extends JpaRepository<Loan, String> {

    // ─── Existing Derived Queries (untouched) ────────────────────────────────

    List<Loan> findByCustomer_CustomerId(String customerId);

    List<Loan> findByStatus(Loan.LoanStatus status);

    List<Loan> findByLoanType(Loan.LoanType loanType);

    List<Loan> findByCustomer_CustomerIdAndStatus(String customerId, Loan.LoanStatus status);

    List<Loan> findByLoanAmountGreaterThan(BigDecimal amount);

    // ─── Existing JPQL Queries (untouched) ───────────────────────────────────

    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.customer.customerId = :customerId")
    BigDecimal getTotalOutstandingBalance(@Param("customerId") String customerId);

    @Query("SELECT l FROM Loan l WHERE l.customer.email = :email")
    List<Loan> findLoansByCustomerEmail(@Param("email") String email);

    @Query(value = "SELECT * FROM loans ORDER BY loan_amount DESC LIMIT 5", nativeQuery = true)
    List<Loan> findTop5LargestLoans();

    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> countLoansByStatus();

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    // Pagination support for existing list queries
    // Usage: loanRepository.findByCustomer_CustomerId(customerId, PageRequest.of(0, 10))
    Page<Loan> findByCustomer_CustomerId(String customerId, Pageable pageable);

    Page<Loan> findByStatus(Loan.LoanStatus status, Pageable pageable);

    // JOIN FETCH — loads loan + customer in a single query, avoids N+1
    // Use this whenever you need to display loan details alongside customer info
    @Query("""
            SELECT l FROM Loan l
            JOIN FETCH l.customer c
            WHERE l.loanId = :loanId
            """)
    Optional<Loan> findByIdWithCustomer(@Param("loanId") String loanId);

    // Overdue loans — ACTIVE loans whose end_date has passed
    // Used for: collections dashboard, sending overdue notifications
    @Query("""
            SELECT l FROM Loan l
            WHERE l.status = 'ACTIVE'
              AND l.endDate < :today
            """)
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    // Customers with overdue loans — distinct to avoid duplicates
    @Query("""
            SELECT DISTINCT l.customer FROM Loan l
            WHERE l.status = 'ACTIVE'
              AND l.endDate < :today
            """)
    List<com.mortgage.mortgage_service.entity.Customer> findCustomersWithOverdueLoans(
            @Param("today") LocalDate today);

    // Top N loans by amount — dynamic N, unlike the hardcoded native query above
    // Usage: loanRepository.findTopLoansByAmount(PageRequest.of(0, N))
    @Query("SELECT l FROM Loan l ORDER BY l.loanAmount DESC")
    List<Loan> findTopLoansByAmount(Pageable pageable);

    // Loan count grouped by status — typed result using Object[]
    // index 0 = LoanStatus, index 1 = count
    // Tip: on Day 3 you can project this into a proper DTO instead of Object[]
    @Query("""
            SELECT l.status, COUNT(l), SUM(l.outstandingBalance)
            FROM Loan l
            GROUP BY l.status
            """)
    List<Object[]> getLoanSummaryByStatus();

    // Bulk update — marks overdue ACTIVE loans as DEFAULTED
    // @Modifying required for UPDATE/DELETE JPQL
    // @Transactional must be on the calling service method
    @Modifying
    @Query("""
            UPDATE Loan l SET l.status = 'DEFAULTED'
            WHERE l.status = 'ACTIVE'
              AND l.endDate < :today
            """)
    int markOverdueLoansAsDefaulted(@Param("today") LocalDate today);

    // Monthly payment summary — groups outstanding balance by year+month of start date
    // Prep for Kafka payment events on Day 4
    @Query(value = """
            SELECT
                EXTRACT(YEAR  FROM start_date) AS year,
                EXTRACT(MONTH FROM start_date) AS month,
                COUNT(*)                        AS loan_count,
                SUM(outstanding_balance)        AS total_outstanding
            FROM loans
            WHERE status = 'ACTIVE'
            GROUP BY year, month
            ORDER BY year, month
            """, nativeQuery = true)
    List<Object[]> getMonthlySummary();
}