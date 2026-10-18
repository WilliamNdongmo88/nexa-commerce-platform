package com.microservices.notification.event.consumer;

import com.microservices.notification.entity.Notification;
import com.microservices.notification.event.dto.PaymentEvent;
import com.microservices.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = {
                    "${app.kafka.topics.payment-completed}",
                    "${app.kafka.topics.payment-failed}"
            },
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=com.microservices.notification.event.dto.PaymentEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void onPaymentEvent(PaymentEvent event, Acknowledgment ack) {
        try {
            log.info("Received payment event: type={}, paymentId={}",
                    event.eventType(), event.aggregateId());

            String subject;
            String body;

            if ("payment.payment.completed".equals(event.eventType())) {
                subject = "Paiement confirmé";
                body = String.format("Bonjour,%n%nVotre paiement de %s %s a été confirmé.%n" +
                                "Transaction : %s%n",
                        event.payload().amount(), event.payload().currency(),
                        event.payload().paymentId());
            } else {
                subject = "Échec de paiement";
                body = String.format("Bonjour,%n%nVotre paiement a échoué : %s%n",
                        event.payload().errorMessage());
            }

            Notification notification = Notification.builder()
                    .type(event.eventType().toUpperCase().replace(".", "_"))
                    .recipientId(event.payload().userId())
                    .recipientEmail(event.payload().userEmail())
                    .subject(subject)
                    .body(body)
                    .referenceId(event.payload().paymentId())
                    .referenceType("PAYMENT")
                    .eventId(event.eventId())
                    .build();

            notificationService.createNotification(notification);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
        }
    }
}