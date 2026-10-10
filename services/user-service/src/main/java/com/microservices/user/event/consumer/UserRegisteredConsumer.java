package com.microservices.user.event.consumer;

import com.microservices.user.event.UserRegisteredEvent;
import com.microservices.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = false    // ← désactivé par défaut tant que Kafka n'est pas là
)
public class UserRegisteredConsumer {

    private final UserService userService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: {}", event);

        if (event.payload() == null) {
            log.warn("Empty payload, skipping");
            return;
        }

        UserRegisteredEvent.Payload payload = event.payload();
        userService.createProfile(
                payload.userId(),
                payload.email(),
                payload.firstName(),
                payload.lastName()
        );
    }
}