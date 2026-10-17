package com.microservices.order.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        String status,
        BigDecimal totalAmount,
        String currency,
        String shippingAddress,
        List<ItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record ItemResponse(
            UUID id,
            UUID productId,
            String productSku,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {}
}