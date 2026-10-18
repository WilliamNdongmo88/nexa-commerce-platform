package com.microservices.notification.event.consumer;

import com.microservices.notification.entity.Notification;
import com.microservices.notification.event.dto.UserRegisteredEvent;
import com.microservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=com.microservices.notification.event.dto.UserRegisteredEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void onUserRegistered(UserRegisteredEvent event, Acknowledgment ack) {
        try {
            log.info("Received user registered event: userId={}", event.aggregateId());

            Notification notification = Notification.builder()
                    .type("USER_REGISTERED")
                    .recipientId(event.payload().userId())
                    .recipientEmail(event.payload().email())
                    .subject("Bienvenue !")
                    .body(String.format("Bonjour %s %s,%n%nBienvenue sur notre plateforme !%n",
                            event.payload().firstName(), event.payload().lastName()))
                    .referenceId(event.payload().userId())
                    .referenceType("USER")
                    .eventId(event.eventId())
                    .build();

            notificationService.createNotification(notification);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process user registered event: {}", e.getMessage(), e);
        }
    }
}