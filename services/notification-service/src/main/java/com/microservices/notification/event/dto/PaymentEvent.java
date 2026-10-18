package com.microservices.notification.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
        UUID eventId,
        String eventType,
        String eventVersion,
        UUID aggregateId,
        Instant occurredAt,
        Payload payload
) {
    public record Payload(
            UUID paymentId,
            UUID orderId,
            UUID userId,
            String userEmail,
            BigDecimal amount,
            String currency,
            String status,
            String errorMessage
    ) {}
}