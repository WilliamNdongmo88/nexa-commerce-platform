package com.microservices.notification.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        String eventType,
        String eventVersion,
        UUID aggregateId,
        Instant occurredAt,
        Payload payload
) {
    public record Payload(
            UUID orderId,
            String orderNumber,
            UUID userId,
            String userEmail,
            String status,
            BigDecimal totalAmount,
            String currency
    ) {}
}