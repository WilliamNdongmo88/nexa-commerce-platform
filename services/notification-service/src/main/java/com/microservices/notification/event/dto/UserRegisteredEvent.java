package com.microservices.notification.event.dto;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        String eventType,
        String eventVersion,
        UUID aggregateId,
        Instant occurredAt,
        Payload payload
) {
    public record Payload(
            UUID userId,
            String email,
            String firstName,
            String lastName
    ) {}
}