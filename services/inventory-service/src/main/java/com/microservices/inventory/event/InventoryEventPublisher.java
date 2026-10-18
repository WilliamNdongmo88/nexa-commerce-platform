package com.microservices.inventory.event;

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
public class InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.stock-reserved:inventory.stock.reserved}")
    private String stockReservedTopic;

    @Value("${app.kafka.topics.stock-released:inventory.stock.released}")
    private String stockReleasedTopic;

    @Value("${app.kafka.topics.stock-failed:inventory.stock.failed}")
    private String stockFailedTopic;

    public void publishStockReserved(UUID productId, int quantity, UUID orderId) {
        publish(stockReservedTopic, productId, quantity, orderId, "inventory.stock.reserved", null);
    }

    public void publishStockReleased(UUID productId, int quantity, UUID orderId) {
        publish(stockReleasedTopic, productId, quantity, orderId, "inventory.stock.released", null);
    }

    public void publishStockFailed(UUID productId, int quantity, UUID orderId, String reason) {
        publish(stockFailedTopic, productId, quantity, orderId, "inventory.stock.failed", reason);
    }

    private void publish(String topic, UUID productId, int quantity, UUID orderId,
                         String eventType, String reason) {
        try {
            var event = new InventoryEventDto(
                    UUID.randomUUID(),
                    eventType,
                    "1.0",
                    productId,
                    Instant.now(),
                    new InventoryEventDto.Payload(productId, quantity, orderId, reason)
            );

            kafkaTemplate.send(topic, productId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {}: {}", eventType, ex.getMessage());
                        } else {
                            log.info("Published {} for product {}", eventType, productId);
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing {}: {}", eventType, e.getMessage(), e);
        }
    }

    public record InventoryEventDto(
            UUID eventId,
            String eventType,
            String eventVersion,
            UUID aggregateId,
            Instant occurredAt,
            Payload payload
    ) {
        public record Payload(
                UUID productId,
                int quantity,
                UUID orderId,
                String reason
        ) {}
    }
}