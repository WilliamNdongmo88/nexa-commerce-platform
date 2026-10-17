package com.microservices.inventory.event.consumer;

import com.microservices.inventory.event.ProductCreatedEvent;
import com.microservices.inventory.service.InventoryService;
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
        matchIfMissing = false
)
public class ProductCreatedConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "${app.kafka.topics.product-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Received ProductCreatedEvent: {}", event);

        if (event.payload() == null) {
            log.warn("Empty payload, skipping");
            return;
        }

        var p = event.payload();
        inventoryService.createInventory(p.productId(), p.sku(), p.initialStock());
    }
}