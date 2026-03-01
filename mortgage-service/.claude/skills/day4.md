# Day 4 Goals — Advanced Topics

## Topics to Cover
- Kafka producer/consumer (payment events)
- AOP — logging, timing, audit aspects
- @ControllerAdvice deep dive
- WebFlux / Mono basics
- Actuator endpoints & custom health
- Microservice concepts
- Spring Cloud overview

## Kafka Flow
Customer makes payment
  → PaymentController
  → PaymentService
  → KafkaProducer publishes "payment-topic"
  → KafkaConsumer listens
  → Updates loan outstanding balance
  → Sends notification (mock)