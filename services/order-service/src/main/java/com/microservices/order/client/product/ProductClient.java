package com.microservices.order.client.product;

import com.microservices.order.dto.external.ProductDto;
import com.microservices.order.exception.SagaFailedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductClient {

    private final WebClient.Builder webClientBuilder;

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackGetProduct")
    @Retry(name = "productService")
    public ProductDto getProduct(UUID productId) {
        return webClientBuilder.build()
                .get()
                .uri("http://product-service/api/v1/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .block();
    }

    public ProductDto fallbackGetProduct(UUID productId, Throwable t) {
        log.error("Product service unavailable for product {}: {}", productId, t.getMessage());
        throw new SagaFailedException("Product service unavailable: " + t.getMessage());
    }
}