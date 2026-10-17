package com.microservices.product.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        CategoryInfo category,
        String imageUrl,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public record CategoryInfo(UUID id, String name, String slug) {}
}