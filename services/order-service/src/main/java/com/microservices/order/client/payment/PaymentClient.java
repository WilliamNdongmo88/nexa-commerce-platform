package com.microservices.order.client.payment;

import com.microservices.order.exception.SagaFailedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final WebClient.Builder webClientBuilder;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackCharge")
    public void charge(UUID orderId, UUID userId, BigDecimal amount, String currency, String cardNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("userId", userId);
        body.put("amount", amount);
        body.put("currency", currency);
        if (cardNumber != null && !cardNumber.isBlank()) {
            body.put("cardNumber", cardNumber);
        }

        webClientBuilder.build()
                .post()
                .uri("http://payment-service/internal/v1/payments/charge")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void fallbackCharge(UUID orderId, UUID userId, BigDecimal amount, String currency,
                               String cardNumber, Throwable t) {
        log.error("Payment service unavailable: {}", t.getMessage());
        throw new SagaFailedException("Payment service unavailable: " + t.getMessage());
    }
}