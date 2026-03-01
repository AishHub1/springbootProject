package com.mortgage.mortgage_service.repository;

import com.mortgage.mortgage_service.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    // Derived queries
    List<Loan> findByCustomer_CustomerId(String customerId);

    List<Loan> findByStatus(Loan.LoanStatus status);

    List<Loan> findByLoanType(Loan.LoanType loanType);

    List<Loan> findByCustomer_CustomerIdAndStatus(String customerId, Loan.LoanStatus status);

    // Loans above a certain amount
    List<Loan> findByLoanAmountGreaterThan(BigDecimal amount);

    // JPQL — total outstanding balance for a customer
    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.customer.customerId = :customerId")
    BigDecimal getTotalOutstandingBalance(@Param("customerId") String customerId);

    // JPQL — find loans by customer email
    @Query("SELECT l FROM Loan l WHERE l.customer.email = :email")
    List<Loan> findLoansByCustomerEmail(@Param("email") String email);

    // Native — top 5 largest loans
    @Query(value = "SELECT * FROM loans ORDER BY loan_amount DESC LIMIT 5",
           nativeQuery = true)
    List<Loan> findTop5LargestLoans();

    // Count loans per status
    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> countLoansByStatus();
}