package com.mortgage.mortgage_service.service;

import com.mortgage.mortgage_service.dto.request.CustomerRequest;
import com.mortgage.mortgage_service.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse getCustomerById(String customerId);
    CustomerResponse getCustomerByEmail(String email);
    List<CustomerResponse> getAllCustomers();
    List<CustomerResponse> getActiveCustomers();
    CustomerResponse updateCustomer(String customerId, CustomerRequest request);
    void deleteCustomer(String customerId);
    List<CustomerResponse> searchByName(String name);
}