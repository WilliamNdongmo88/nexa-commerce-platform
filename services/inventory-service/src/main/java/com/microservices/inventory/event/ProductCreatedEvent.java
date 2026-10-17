package com.microservices.inventory.event;

import java.time.Instant;
import java.util.UUID;

public record ProductCreatedEvent(
        UUID eventId,
        String eventType,
        String eventVersion,
        UUID aggregateId,
        Instant occurredAt,
        Payload payload
) {
    public record Payload(
            UUID productId,
            String sku,
            String name,
            int initialStock
    ) {}
}