package com.microservices.order.controller;

import com.microservices.order.dto.request.CreateOrderRequest;
import com.microservices.order.dto.response.OrderResponse;
import com.microservices.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gestion des commandes")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une commande (Saga orchestrée)")
    public OrderResponse create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(userId, request, idempotencyKey);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une commande par ID")
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Lister les commandes de l'utilisateur courant")
    public Page<OrderResponse> list(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderService.listByUser(userId, pageable);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Annuler une commande")
    public OrderResponse cancel(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id
    ) {
        return orderService.cancel(id, userId);
    }
}