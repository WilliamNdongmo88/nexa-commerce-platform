package com.microservices.auth.event;

import com.microservices.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-registered:auth.user.registered}")
    private String userRegisteredTopic;

    public void publishUserRegistered(User user) {
        try {
            var event = new UserRegisteredEventDto(
                    UUID.randomUUID(),
                    "auth.user.registered",
                    "1.0",
                    user.getId(),
                    Instant.now(),
                    new UserRegisteredEventDto.Payload(
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName()
                    )
            );

            kafkaTemplate.send(userRegisteredTopic, user.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish user.registered for {}: {}",
                                    user.getId(), ex.getMessage());
                        } else {
                            log.info("Published user.registered for {} to {}",
                                    user.getId(), userRegisteredTopic);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing user.registered: {}", e.getMessage(), e);
        }
    }

    public record UserRegisteredEventDto(
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
}