package com.microservices.payment.exception;

public class PaymentFailedException extends RuntimeException {

    private final String errorCode;

    public PaymentFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}