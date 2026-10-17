package com.microservices.product.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) {
        super("Product not found: " + id);
    }

    public ProductNotFoundException(String sku) {
        super("Product not found with SKU: " + sku);
    }
}