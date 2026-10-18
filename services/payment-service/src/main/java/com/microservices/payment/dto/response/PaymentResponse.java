package com.microservices.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String status,
        String paymentMethod,
        String transactionId,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {}