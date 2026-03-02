package com.mortgage.mortgage_service.dto.response;

import com.mortgage.mortgage_service.entity.Customer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private Customer.CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;         // added — useful for auditing & caching

    // Loan summary — populated only on detail endpoints (not list endpoints)
    // Avoids loading full loan objects on every customer list call
    private Integer loanCount;              // total loans across all statuses
    private BigDecimal totalOutstandingBalance; // sum of ACTIVE loan balances only

    // Full loan list — only populated when explicitly requested (e.g. GET /customers/{id}/loans)
    // Kept as List<LoanResponse> to match your existing design
    private List<LoanResponse> loans;
}