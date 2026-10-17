package com.microservices.inventory.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdjustStockRequest(
        @NotNull UUID productId,
        @NotNull Integer quantityDelta,    // peut être négatif
        String reason
) {}