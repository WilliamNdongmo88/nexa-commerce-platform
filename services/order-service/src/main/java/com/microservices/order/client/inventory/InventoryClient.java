package com.microservices.order.client.inventory;

import com.microservices.order.exception.SagaFailedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackReserve")
    public void reserve(UUID productId, int quantity, UUID orderId) {
        webClientBuilder.build()
                .post()
                .uri("http://inventory-service/internal/v1/inventory/reserve")
                .bodyValue(Map.of(
                        "productId", productId,
                        "quantity", quantity,
                        "orderId", orderId
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackRelease")
    public void release(UUID productId, int quantity, UUID orderId) {
        webClientBuilder.build()
                .post()
                .uri("http://inventory-service/internal/v1/inventory/release")
                .bodyValue(Map.of(
                        "productId", productId,
                        "quantity", quantity,
                        "orderId", orderId
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void fallbackReserve(UUID productId, int quantity, UUID orderId, Throwable t) {
        log.error("Inventory service unavailable (reserve): {}", t.getMessage());
        throw new SagaFailedException("Inventory service unavailable");
    }

    public void fallbackRelease(UUID productId, int quantity, UUID orderId, Throwable t) {
        log.error("Inventory service unavailable (release): {}", t.getMessage());
        // Ne pas throw ici : compensation best-effort
    }
}