package com.microservices.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        String productSku,
        Integer quantity,
        Integer reserved,
        Integer available,
        Integer lowStockThreshold,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt
) {}