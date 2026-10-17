package com.microservices.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveStockRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer quantity,
        UUID orderId
) {}