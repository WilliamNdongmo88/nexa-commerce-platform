package com.microservices.order.saga.state;

public enum SagaStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}