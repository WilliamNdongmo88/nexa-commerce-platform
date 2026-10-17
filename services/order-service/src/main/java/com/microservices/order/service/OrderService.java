package com.microservices.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.dto.request.CreateOrderRequest;
import com.microservices.order.dto.response.OrderResponse;
import com.microservices.order.entity.IdempotencyKey;
import com.microservices.order.entity.Order;
import com.microservices.order.exception.OrderNotFoundException;
import com.microservices.order.mapper.OrderMapper;
import com.microservices.order.repository.IdempotencyKeyRepository;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.service.saga.PlaceOrderSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PlaceOrderSaga placeOrderSaga;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request, String idempotencyKey) {
        // 1. Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository.findById(idempotencyKey);
            if (existing.isPresent()) {
                try {
                    OrderResponse cached = objectMapper.readValue(
                            existing.get().getResponseBody(), OrderResponse.class);
                    log.info("Idempotency hit for key {} → returning cached response", idempotencyKey);
                    return cached;
                } catch (Exception e) {
                    log.warn("Failed to deserialize cached response: {}", e.getMessage());
                }
            }
        }

        // 2. Exécuter la saga
        List<PlaceOrderSaga.CreateItem> items = request.items().stream()
                .map(i -> new PlaceOrderSaga.CreateItem(i.productId(), i.quantity()))
                .toList();

        Order order = placeOrderSaga.execute(userId, items, request.shippingAddress());
        OrderResponse response = orderMapper.toResponse(order);

        // 3. Sauvegarder la réponse pour l'idempotency
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                IdempotencyKey idem = IdempotencyKey.builder()
                        .key(idempotencyKey)
                        .responseBody(objectMapper.writeValueAsString(response))
                        .httpStatus(201)
                        .build();
                idempotencyKeyRepository.save(idem);
            } catch (Exception e) {
                log.warn("Failed to save idempotency key: {}", e.getMessage());
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listByUser(UUID userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(orderMapper::toResponse);
    }

    @Transactional
    public OrderResponse cancel(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Cannot cancel another user's order");
        }

        if (order.getStatus() == Order.OrderStatus.CONFIRMED
                || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order in status " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderMapper.toResponse(orderRepository.save(order));
    }
}