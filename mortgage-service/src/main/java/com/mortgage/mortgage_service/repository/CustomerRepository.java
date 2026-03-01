package com.mortgage.mortgage_service.repository;

import com.mortgage.mortgage_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    // Spring Data JPA — derived query methods (auto-generates SQL from method name!)
    Optional<Customer> findByEmail(String email);

    List<Customer> findByStatus(Customer.CustomerStatus status);

    boolean existsByEmail(String email);

    List<Customer> findByFirstNameContainingIgnoreCase(String firstName);

    // JPQL — uses Java class names, not table names
    @Query("SELECT c FROM Customer c WHERE c.firstName = :firstName AND c.lastName = :lastName")
    List<Customer> findByFullName(@Param("firstName") String firstName,
                                  @Param("lastName") String lastName);

    // Native SQL query
    @Query(value = "SELECT * FROM customers WHERE status = 'ACTIVE' ORDER BY created_at DESC",
           nativeQuery = true)
    List<Customer> findAllActiveCustomers();

    // Customers who have at least one loan
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.loans l")
    List<Customer> findCustomersWithLoans();
}