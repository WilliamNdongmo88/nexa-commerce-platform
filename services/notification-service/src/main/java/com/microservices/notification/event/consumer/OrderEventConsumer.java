package com.microservices.notification.event.consumer;

import com.microservices.notification.entity.Notification;
import com.microservices.notification.event.dto.OrderEvent;
import com.microservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = {
                    "${app.kafka.topics.order-created}",
                    "${app.kafka.topics.order-confirmed}",
                    "${app.kafka.topics.order-cancelled}"
            },
            groupId = "${spring.kafka.consumer.group-id}",
            // ⭐ On dit au JsonDeserializer quel type utiliser (FQDN)
            properties = {
                    "spring.json.value.default.type=com.microservices.notification.event.dto.OrderEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void onOrderEvent(OrderEvent event, Acknowledgment ack) {
        try {
            log.info("Received order event: type={}, orderId={}",
                    event.eventType(), event.aggregateId());

            String subject = buildSubject(event);
            String body = buildBody(event);

            Notification notification = Notification.builder()
                    .type(event.eventType().toUpperCase().replace(".", "_"))
                    .recipientId(event.payload().userId())
                    .recipientEmail(event.payload().userEmail())
                    .subject(subject)
                    .body(body)
                    .referenceId(event.payload().orderId())
                    .referenceType("ORDER")
                    .eventId(event.eventId())
                    .build();

            notificationService.createNotification(notification);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage(), e);
        }
    }

    private String buildSubject(OrderEvent event) {
        return switch (event.eventType()) {
            case "order.order.created" -> "Commande créée - " + event.payload().orderNumber();
            case "order.order.confirmed" -> "Commande confirmée - " + event.payload().orderNumber();
            case "order.order.cancelled" -> "Commande annulée - " + event.payload().orderNumber();
            default -> "Mise à jour de commande " + event.payload().orderNumber();
        };
    }

    private String buildBody(OrderEvent event) {
        return String.format(
                "Bonjour,%n%nVotre commande %s est maintenant au statut : %s.%n" +
                        "Montant : %s %s%n%nCordialement,%nL'équipe",
                event.payload().orderNumber(),
                event.payload().status(),
                event.payload().totalAmount(),
                event.payload().currency()
        );
    }
}