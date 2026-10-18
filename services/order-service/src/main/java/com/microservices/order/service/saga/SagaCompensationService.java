package com.microservices.order.service.saga;

import com.microservices.order.client.inventory.InventoryClient;
import com.microservices.order.entity.Order;
import com.microservices.order.entity.SagaState;
import com.microservices.order.event.OrderEventPublisher;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.SagaStateRepository;
import com.microservices.order.saga.step.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaCompensationService {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * Compense la saga : libère le stock et marque la commande CANCELLED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(UUID orderId, List<PlaceOrderSaga.ReservedStock> reservedStock, String reason) {
        log.info("Compensating saga for order {}: {}", orderId, reason);

        // 1. Libérer le stock
        for (PlaceOrderSaga.ReservedStock r : reservedStock) {
            try {
                inventoryClient.release(r.productId(), r.quantity(), orderId);
                recordStep(orderId, SagaStep.RESERVE_STOCK, SagaState.SagaStatus.COMPENSATED);
                log.info("Released stock for product {}", r.productId());
            } catch (Exception compEx) {
                log.error("Failed to release stock for product {}: {}",
                        r.productId(), compEx.getMessage());
                recordStep(orderId, SagaStep.RESERVE_STOCK, SagaState.SagaStatus.FAILED);
            }
        }

        // 2. Marquer la commande CANCELLED
        // On récupère l'entité fraîche depuis la BDD dans cette nouvelle transaction
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.saveAndFlush(order);
            orderEventPublisher.publishOrderCancelled(order, null, "Payment failed");
            log.info("Order {} marked as CANCELLED", orderId);
        } else {
            log.error("Order {} not found during compensation!", orderId);
        }

        // 3. Enregistrer l'étape de paiement échouée
        recordStep(orderId, SagaStep.CHARGE_PAYMENT, SagaState.SagaStatus.FAILED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStep(UUID orderId, SagaStep step, SagaState.SagaStatus status) {
        SagaState state = SagaState.builder()
                .orderId(orderId)
                .step(step.name())
                .status(status)
                .build();
        sagaStateRepository.saveAndFlush(state);
    }
}