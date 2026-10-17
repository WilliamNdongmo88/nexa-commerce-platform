package com.microservices.order.client.user;

import com.microservices.order.exception.SagaFailedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackExists")
    public boolean userExists(UUID userId) {
        try {
            webClientBuilder.build()
                    .get()
                    .uri("http://user-service/api/v1/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean fallbackExists(UUID userId, Throwable t) {
        log.error("User service unavailable: {}", t.getMessage());
        throw new SagaFailedException("User service unavailable");
    }
}