package com.mortgage.mortgage_service.dto.response;

import com.mortgage.mortgage_service.entity.Customer;
import lombok.*;

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
    private List<LoanResponse> loans;
}