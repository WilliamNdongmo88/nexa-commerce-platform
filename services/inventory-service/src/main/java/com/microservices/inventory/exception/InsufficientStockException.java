package com.microservices.inventory.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(UUID productId, int requested, int available) {
        super(String.format(
                "Insufficient stock for product %s: requested %d, available %d",
                productId, requested, available));
    }
}