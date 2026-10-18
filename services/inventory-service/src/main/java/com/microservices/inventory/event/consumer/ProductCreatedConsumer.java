package com.microservices.inventory.event.consumer;

import com.microservices.inventory.event.ProductCreatedEvent;
import com.microservices.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCreatedConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "${app.kafka.topics.product-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type=com.microservices.inventory.event.ProductCreatedEvent",
                    "spring.json.use.type.headers=false"
            }
    )
    public void onProductCreated(ProductCreatedEvent event, Acknowledgment ack) {
        try {
            log.info("Received ProductCreatedEvent: productId={}, sku={}",
                    event.aggregateId(),
                    event.payload() != null ? event.payload().sku() : null);

            if (event.payload() == null) {
                log.warn("Empty payload, skipping");
                ack.acknowledge();
                return;
            }

            var p = event.payload();
            inventoryService.createInventory(p.productId(), p.sku(), p.initialStock());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process ProductCreatedEvent: {}", e.getMessage(), e);
        }
    }
}