package com.microservices.gateway.filter;

import com.microservices.gateway.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final Tracer tracer;
    private final Propagator propagator;

    // Endpoints publics
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    );

    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/v1/products",
            "/api/v1/inventory"
    );

    private static final List<String> ADMIN_ONLY_PREFIXES = List.of(
            "/api/v1/products",
            "/api/v1/inventory"
    );

    private static final List<String> ADMIN_ONLY_METHODS = List.of(
            "POST", "PUT", "DELETE", "PATCH"
    );

    public JwtAuthenticationFilter(JwtService jwtService, Tracer tracer, Propagator propagator) {
        this.jwtService = jwtService;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        log.info("REQUEST: {} {} | traceId={} | spanId={}",
                method, path,
                tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "NO_TRACE",
                tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "NO_SPAN");

        // 1. Préparer les headers de tracing
        HttpHeaders tracingHeaders = new HttpHeaders();
        tracingHeaders.putAll(request.getHeaders());
        if (tracer.currentSpan() != null) {
            propagator.inject(tracer.currentSpan().context(), tracingHeaders, (carrier, key, value) -> carrier.set(key, value));
        }

        // 2. Endpoints publics
        if (isPublic(path, method)) {
            log.debug("Public endpoint, bypass JWT: {} {}", method, path);
            ServerHttpRequest mutated = new ServerHttpRequestDecorator(request) {
                @Override
                public HttpHeaders getHeaders() { return tracingHeaders; }
            };
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // 3. Endpoints protégés
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header on {}", path);
            return unauthorized(exchange, "Missing Bearer token");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAndExtract(token);
            if (jwtService.isExpired(claims)) {
                return unauthorized(exchange, "Token expired");
            }

            List<String> roles = jwtService.extractRoles(claims);
            String userId = jwtService.extractUserId(claims);

            if (requiresAdmin(path, method) && !roles.contains("ROLE_ADMIN")) {
                return forbidden(exchange, "Insufficient privileges");
            }

            // 4. Injecter les headers utilisateur ET les headers de tracing
            String traceId = (String) exchange.getAttributes().getOrDefault("traceId", UUID.randomUUID().toString());

            ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
                @Override
                public HttpHeaders getHeaders() {
                    tracingHeaders.add("X-User-Id", userId);
                    tracingHeaders.add("X-User-Email", nullSafe(jwtService.extractEmail(claims)));
                    tracingHeaders.add("X-User-Roles", String.join(",", roles));
                    tracingHeaders.add("X-Trace-Id", traceId);
                    return tracingHeaders;
                }
            };

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return unauthorized(exchange, "Invalid token");
        }
    }

    private boolean isPublic(String path, String method) {
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return true;
        if ("GET".equals(method)) return PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith);
        return false;
    }

    private boolean requiresAdmin(String path, String method) {
        return ADMIN_ONLY_METHODS.contains(method) && ADMIN_ONLY_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", reason);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("X-Auth-Error", reason);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}