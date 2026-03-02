package com.mortgage.mortgage_service.repository;

import com.mortgage.mortgage_service.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    // ─── Existing Derived Queries (untouched) ────────────────────────────────

    Optional<Customer> findByEmail(String email);

    List<Customer> findByStatus(Customer.CustomerStatus status);

    boolean existsByEmail(String email);

    List<Customer> findByFirstNameContainingIgnoreCase(String firstName);

    // ─── Existing JPQL & Native Queries (untouched) ──────────────────────────

    @Query("SELECT c FROM Customer c WHERE c.firstName = :firstName AND c.lastName = :lastName")
    List<Customer> findByFullName(@Param("firstName") String firstName,
                                  @Param("lastName") String lastName);

    @Query(value = "SELECT * FROM customers WHERE status = 'ACTIVE' ORDER BY created_at DESC",
           nativeQuery = true)
    List<Customer> findAllActiveCustomers();

    @Query("SELECT DISTINCT c FROM Customer c JOIN c.loans l")
    List<Customer> findCustomersWithLoans();

    // ─── Day 2 Additions ─────────────────────────────────────────────────────

    // Pagination support for existing list queries
    // Usage: customerRepository.findByStatus(status, PageRequest.of(0, 10))
    Page<Customer> findByStatus(Customer.CustomerStatus status, Pageable pageable);

    // Paginated version of findAll — use this in controllers instead of findAll()
    // findAll(Pageable) is already inherited from JpaRepository, no need to declare it

    // JOIN FETCH — loads customer + all their loans in a single query
    // Use this on the customer detail page where you need to show loans too
    // Without this, accessing customer.getLoans() would fire a second SQL query (N+1)
    @Query("""
            SELECT DISTINCT c FROM Customer c
            LEFT JOIN FETCH c.loans
            WHERE c.customerId = :customerId
            """)
    Optional<Customer> findByIdWithLoans(@Param("customerId") String customerId);

    // Search across both first AND last name, case-insensitive
    // More useful than searching first name only
    // Usage: ?search=john → matches "John Smith" and "Mary Johnson"
    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<Customer> searchByName(@Param("name") String name, Pageable pageable);

    // Total outstanding balance across all ACTIVE loans for a customer
    // Returns null if customer has no active loans — handle in service layer
    @Query("""
            SELECT SUM(l.outstandingBalance) FROM Customer c
            JOIN c.loans l
            WHERE c.customerId = :customerId
              AND l.status = 'ACTIVE'
            """)
    BigDecimal getTotalOutstandingBalanceByCustomer(@Param("customerId") String customerId);

    // Loan count per customer — useful for customer summary cards
    @Query("""
            SELECT COUNT(l) FROM Customer c
            JOIN c.loans l
            WHERE c.customerId = :customerId
            """)
    long getLoanCountByCustomer(@Param("customerId") String customerId);

    // Dashboard metric — how many customers have at least one ACTIVE loan
    @Query("""
            SELECT COUNT(DISTINCT c) FROM Customer c
            JOIN c.loans l
            WHERE l.status = 'ACTIVE'
            """)
    long countCustomersWithActiveLoans();

    // Customers with more than N loans — risk/exposure monitoring
    @Query("""
            SELECT c FROM Customer c
            WHERE SIZE(c.loans) > :loanCount
            """)
    List<Customer> findCustomersWithMoreThanNLoans(@Param("loanCount") int loanCount);
}