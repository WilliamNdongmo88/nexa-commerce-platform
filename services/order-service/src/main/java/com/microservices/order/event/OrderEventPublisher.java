package com.microservices.order.event;

import com.microservices.order.entity.Order;
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
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-created:order.order.created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.topics.order-confirmed:order.order.confirmed}")
    private String orderConfirmedTopic;

    @Value("${app.kafka.topics.order-cancelled:order.order.cancelled}")
    private String orderCancelledTopic;

    public void publishOrderCreated(Order order, String userEmail) {
        publish(orderCreatedTopic, order, userEmail, "order.order.created");
    }

    public void publishOrderConfirmed(Order order, String userEmail) {
        publish(orderConfirmedTopic, order, userEmail, "order.order.confirmed");
    }

    public void publishOrderCancelled(Order order, String userEmail, String reason) {
        publish(orderCancelledTopic, order, userEmail, "order.order.cancelled");
    }

    private void publish(String topic, Order order, String userEmail, String eventType) {
        try {
            var event = new OrderEventDto(
                    UUID.randomUUID(),
                    eventType,
                    "1.0",
                    order.getId(),
                    Instant.now(),
                    new OrderEventDto.Payload(
                            order.getId(),
                            order.getOrderNumber(),
                            order.getUserId(),
                            userEmail,
                            order.getStatus().name(),
                            order.getTotalAmount(),
                            order.getCurrency()
                    )
            );

            kafkaTemplate.send(topic, order.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for order {}: {}",
                                    eventType, order.getId(), ex.getMessage());
                        } else {
                            log.info("Published {} for order {} to {}",
                                    eventType, order.getId(), topic);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception while publishing {}: {}", eventType, e.getMessage(), e);
        }
    }

    public record OrderEventDto(
            UUID eventId,
            String eventType,
            String eventVersion,
            UUID aggregateId,
            Instant occurredAt,
            Payload payload
    ) {
        public record Payload(
                UUID orderId,
                String orderNumber,
                UUID userId,
                String userEmail,
                String status,
                java.math.BigDecimal totalAmount,
                String currency
        ) {}
    }
}