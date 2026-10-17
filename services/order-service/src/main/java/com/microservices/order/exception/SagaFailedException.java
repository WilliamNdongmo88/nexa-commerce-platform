package com.microservices.order.exception;

public class SagaFailedException extends RuntimeException {
    public SagaFailedException(String message) {
        super(message);
    }

    public SagaFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}