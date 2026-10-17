package com.microservices.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @DecimalMin("0.00") BigDecimal price,
        @Size(min = 3, max = 3) String currency,
        UUID categoryId,
        @Size(max = 500) String imageUrl,
        Boolean active
) {}