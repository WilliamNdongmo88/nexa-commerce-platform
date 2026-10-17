package com.microservices.order.saga.step;

public enum SagaStep {
    VALIDATE_USER,
    VALIDATE_PRODUCTS,
    RESERVE_STOCK,
    CHARGE_PAYMENT,
    CONFIRM_ORDER
}