package com.microservices.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

 /**
 * Utile pour le debug et pour plus tard le tracing distribué.
 */

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        // Injecte le traceId dans la requête ET la réponse
        String finalTraceId = traceId;
        exchange.getResponse().getHeaders().add("X-Trace-Id", finalTraceId);

        var mutatedRequest = exchange.getRequest().mutate()
                .header("X-Trace-Id", finalTraceId)
                .build();

        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    log.info("[{}] {} {} → {} ({} ms)",
                            finalTraceId, method, path,
                            exchange.getResponse().getStatusCode(),
                            duration);
                });
    }

    @Override
    public int getOrder() {
        return -200;   // Avant tout
    }
}