package com.microservices.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<ItemRequest> items,

        @NotBlank(message = "Shipping address is required")
        @Size(max = 1000)
        String shippingAddress
) {
    public record ItemRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity
    ) {}
}