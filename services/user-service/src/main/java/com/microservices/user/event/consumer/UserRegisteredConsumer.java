package com.microservices.user.event.consumer;

import com.microservices.user.event.UserRegisteredEvent;
import com.microservices.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final UserService userService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=com.microservices.user.event.UserRegisteredEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void onUserRegistered(UserRegisteredEvent event, Acknowledgment ack) {
        try {
            log.info("Received UserRegisteredEvent: userId={}", event.aggregateId());

            if (event.payload() == null) {
                log.warn("Empty payload, skipping");
                ack.acknowledge();
                return;
            }

            UserRegisteredEvent.Payload payload = event.payload();
            userService.createProfile(
                    payload.userId(),
                    payload.email(),
                    payload.firstName(),
                    payload.lastName()
            );
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process UserRegisteredEvent: {}", e.getMessage(), e);
            // ne pas ack → retry
        }
    }
}