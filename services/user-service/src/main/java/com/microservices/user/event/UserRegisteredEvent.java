package com.microservices.user.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement publié par auth-service lors de l'inscription.
 * Consommé par user-service pour créer le profil.
 *
 * ⚠️ Kafka sera installé à l'étape 13.
 * Le code est ici prêt pour l'intégration.
 */
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