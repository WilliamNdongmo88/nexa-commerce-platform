package com.microservices.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_ATTR = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Récupérer ou générer le traceId
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        // 2. Stocker dans les attributs de l'échange (accessible par les autres filtres)
        exchange.getAttributes().put(TRACE_ID_ATTR, traceId);

        // 3. Ajouter le traceId aux headers de RÉPONSE (pas de mutation de requête !)
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

        // 4. Logger la requête
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String finalTraceId = traceId;

        return chain.filter(exchange)
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    log.info("[{}] {} {} → {} ({} ms)",
                            finalTraceId,
                            method,
                            path,
                            exchange.getResponse().getStatusCode(),
                            duration);
                });
    }

    @Override
    public int getOrder() {
        return -200;   // Avant tout (mais pas critique ici)
    }
}