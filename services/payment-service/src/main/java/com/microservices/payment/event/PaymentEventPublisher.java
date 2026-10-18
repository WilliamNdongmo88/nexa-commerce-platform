package com.microservices.payment.event;

import com.microservices.payment.entity.Payment;
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
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.payment-completed:payment.payment.completed}")
    private String completedTopic;

    @Value("${app.kafka.topics.payment-failed:payment.payment.failed}")
    private String failedTopic;

    public void publishCompleted(Payment payment) {
        publish(completedTopic, payment, "payment.payment.completed");
    }

    public void publishFailed(Payment payment) {
        publish(failedTopic, payment, "payment.payment.failed");
    }

    private void publish(String topic, Payment payment, String eventType) {
        try {
            var event = new PaymentEventDto(
                    UUID.randomUUID(),
                    eventType,
                    "1.0",
                    payment.getId(),
                    Instant.now(),
                    new PaymentEventDto.Payload(
                            payment.getId(),
                            payment.getOrderId(),
                            payment.getUserId(),
                            null,   // email : à récupérer étape 13
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getStatus().name(),
                            payment.getErrorMessage()
                    )
            );

            kafkaTemplate.send(topic, payment.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {}: {}", eventType, ex.getMessage());
                        } else {
                            log.info("Published {} for payment {}", eventType, payment.getId());
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing {}: {}", eventType, e.getMessage(), e);
        }
    }

    public record PaymentEventDto(
            UUID eventId,
            String eventType,
            String eventVersion,
            UUID aggregateId,
            Instant occurredAt,
            Payload payload
    ) {
        public record Payload(
                UUID paymentId,
                UUID orderId,
                UUID userId,
                String userEmail,
                java.math.BigDecimal amount,
                String currency,
                String status,
                String errorMessage
        ) {}
    }
}