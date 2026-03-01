# Mortgage Servicing System — Project Context

## Stack
- Java 17, Spring Boot 3.5.x, Maven
- PostgreSQL (mortgage_db, user: postgres, no password)
- Kafka (Day 4)
- Lombok, JPA, Spring Security, Actuator, Validation

## Package
com.mortgage.mortgage_service

## Domain
- Customer (1) ──── (Many) Loan
- Loan (1) ──── (Many) Payment ← Kafka on Day 4

## Project Structure
src/main/java/com/mortgage/mortgage_service/
├── config/
│   └── SecurityConfig.java          ← open, all permitted for now
├── controller/
│   ├── CustomerController.java      ← /api/v1/customers
│   └── LoanController.java          ← /api/v1/loans
├── service/
│   ├── CustomerService.java         ← interface
│   ├── LoanService.java             ← interface
│   └── impl/
│       ├── CustomerServiceImpl.java ← business logic
│       └── LoanServiceImpl.java     ← business logic
├── repository/
│   ├── CustomerRepository.java
│   └── LoanRepository.java
├── entity/
│   ├── Customer.java                ← UUID PK, OneToMany Loans
│   └── Loan.java                    ← UUID PK, ManyToOne Customer
├── dto/
│   ├── request/
│   │   ├── CustomerRequest.java
│   │   └── LoanRequest.java
│   └── response/
│       ├── CustomerResponse.java
│       └── LoanResponse.java
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice
│   └── ErrorResponse.java
└── kafka/
    ├── producer/
    └── consumer/

## What's Done
- [x] Day 1 — Entities, Repositories, Services, Controllers, Exception Handling
- [ ] Day 2 — JPA deep dive, relationships, queries, pagination, profiles
- [ ] Day 3 — Spring Security, JWT, roles, method-level auth
- [ ] Day 4 — Kafka payments, AOP, WebFlux, Actuators, Microservices