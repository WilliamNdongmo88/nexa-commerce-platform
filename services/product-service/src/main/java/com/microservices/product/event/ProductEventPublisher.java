package com.microservices.product.event;

import com.microservices.product.entity.Product;
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
public class ProductEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.product-created:product.product.created}")
    private String productCreatedTopic;

    @Value("${app.kafka.topics.product-updated:product.product.updated}")
    private String productUpdatedTopic;

    public void publishProductCreated(Product product, int initialStock) {
        try {
            var event = new ProductEventDto(
                    UUID.randomUUID(),
                    "product.product.created",
                    "1.0",
                    product.getId(),
                    Instant.now(),
                    new ProductEventDto.Payload(
                            product.getId(),
                            product.getSku(),
                            product.getName(),
                            initialStock
                    )
            );

            kafkaTemplate.send(productCreatedTopic, product.getId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish product.created for {}: {}",
                                    product.getId(), ex.getMessage());
                        } else {
                            log.info("Published product.created for {} (sku={})",
                                    product.getId(), product.getSku());
                        }
                    });
        } catch (Exception e) {
            log.error("Exception publishing product.created: {}", e.getMessage(), e);
        }
    }

    public void publishProductUpdated(Product product) {
        try {
            var event = new ProductEventDto(
                    UUID.randomUUID(),
                    "product.product.updated",
                    "1.0",
                    product.getId(),
                    Instant.now(),
                    new ProductEventDto.Payload(
                            product.getId(),
                            product.getSku(),
                            product.getName(),
                            0
                    )
            );

            kafkaTemplate.send(productUpdatedTopic, product.getId().toString(), event);
        } catch (Exception e) {
            log.error("Exception publishing product.updated: {}", e.getMessage(), e);
        }
    }

    public record ProductEventDto(
            UUID eventId,
            String eventType,
            String eventVersion,
            UUID aggregateId,
            Instant occurredAt,
            Payload payload
    ) {
        public record Payload(
                UUID productId,
                String sku,
                String name,
                int initialStock
        ) {}
    }
}