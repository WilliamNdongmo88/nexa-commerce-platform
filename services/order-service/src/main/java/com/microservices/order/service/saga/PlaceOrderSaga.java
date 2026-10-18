package com.microservices.order.service.saga;

import com.microservices.order.client.inventory.InventoryClient;
import com.microservices.order.client.payment.PaymentClient;
import com.microservices.order.client.product.ProductClient;
import com.microservices.order.client.user.UserClient;
import com.microservices.order.dto.external.ProductDto;
import com.microservices.order.entity.Order;
import com.microservices.order.entity.OrderItem;
import com.microservices.order.entity.SagaState;
import com.microservices.order.event.OrderEventPublisher;
import com.microservices.order.exception.SagaFailedException;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.SagaStateRepository;
import com.microservices.order.saga.step.SagaStep;
import com.microservices.order.saga.state.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceOrderSaga {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final SagaCompensationService compensationService;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * Exécute la saga. IMPORTANT : cette méthode N'EST PAS transactionnelle
     * (pas de @Transactional), sinon les compensations seraient rollback-ées.
     */
    public Order execute(UUID userId,
                         List<CreateItem> items,
                         String shippingAddress,
                         String cardNumber) {

        Order order = null;
        List<ReservedStock> reservedStock = new ArrayList<>();

        try {
            // Étape 1 : VALIDATE_USER
            recordStep(null, SagaStep.VALIDATE_USER, SagaStatus.STARTED);
            if (!userClient.userExists(userId)) {
                throw new SagaFailedException("User not found: " + userId);
            }
            recordStep(null, SagaStep.VALIDATE_USER, SagaStatus.COMPLETED);

            // Étape 2 : VALIDATE_PRODUCTS + calcul du total
            recordStep(null, SagaStep.VALIDATE_PRODUCTS, SagaStatus.STARTED);
            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            String currency = "EUR";

            for (CreateItem item : items) {
                ProductDto product = productClient.getProduct(item.productId());
                if (product == null || !Boolean.TRUE.equals(product.getActive())) {
                    throw new SagaFailedException("Product unavailable: " + item.productId());
                }
                OrderItem oi = OrderItem.builder()
                        .productId(product.getId())
                        .productSku(product.getSku())
                        .productName(product.getName())
                        .quantity(item.quantity())
                        .unitPrice(product.getPrice())
                        .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())))
                        .build();
                orderItems.add(oi);
                total = total.add(oi.getTotalPrice());
                currency = product.getCurrency();
            }
            recordStep(null, SagaStep.VALIDATE_PRODUCTS, SagaStatus.COMPLETED);

            // Étape 3 : Créer la commande (PENDING)
            order = Order.builder()
                    .orderNumber(generateOrderNumber())
                    .userId(userId)
                    .status(Order.OrderStatus.PENDING)
                    .totalAmount(total)
                    .currency(currency)
                    .shippingAddress(shippingAddress)
                    .build();
            for (OrderItem item : orderItems) {
                order.addItem(item);
            }
            order = orderRepository.saveAndFlush(order);
            orderEventPublisher.publishOrderCreated(order, null);
            log.info("Order created: {} ({})", order.getOrderNumber(), order.getId());

            // Étape 4 : RESERVE_STOCK
            recordStep(order.getId(), SagaStep.RESERVE_STOCK, SagaStatus.STARTED);
            for (OrderItem item : order.getItems()) {
                inventoryClient.reserve(item.getProductId(), item.getQuantity(), order.getId());
                reservedStock.add(new ReservedStock(item.getProductId(), item.getQuantity()));
            }
            order.setStatus(Order.OrderStatus.STOCK_RESERVED);
            order = orderRepository.save(order);
            recordStep(order.getId(), SagaStep.RESERVE_STOCK, SagaStatus.COMPLETED);

            // Étape 5 : CHARGE_PAYMENT
            recordStep(order.getId(), SagaStep.CHARGE_PAYMENT, SagaStatus.STARTED);
            try {
                paymentClient.charge(order.getId(), userId, total, currency, cardNumber);
            } catch (Exception e) {
                log.warn("Payment failed for order {}: {}", order.getId(), e.getMessage());
                recordStep(order.getId(), SagaStep.CHARGE_PAYMENT, SagaStatus.FAILED);
                throw new SagaFailedException("Payment failed: " + e.getMessage(), e);
            }
            order.setStatus(Order.OrderStatus.PAID);
            order = orderRepository.save(order);
            recordStep(order.getId(), SagaStep.CHARGE_PAYMENT, SagaStatus.COMPLETED);

            // Étape 6 : CONFIRM_ORDER
            recordStep(order.getId(), SagaStep.CONFIRM_ORDER, SagaStatus.STARTED);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
            recordStep(order.getId(), SagaStep.CONFIRM_ORDER, SagaStatus.COMPLETED);
            orderEventPublisher.publishOrderConfirmed(order, null);

            log.info("Order confirmed: {}", order.getOrderNumber());
            return order;

        } catch (Exception e) {
            log.error("Saga failed for order {}: {}",
                    order != null ? order.getId() : null, e.getMessage(), e);

            if (order != null) {
                // ⭐ Déléguer à un service dédié (nouvelle transaction)
                compensationService.compensate(order.getId(), reservedStock, e.getMessage());
            }
            throw new SagaFailedException("Order placement failed: " + e.getMessage(), e);
        }
    }

    private void recordStep(UUID orderId, SagaStep step, SagaStatus status) {
        SagaState state = SagaState.builder()
                .orderId(orderId != null ? orderId : UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .step(step.name())
                .status(mapStatus(status))
                .build();
        sagaStateRepository.save(state);
    }

    private SagaState.SagaStatus mapStatus(SagaStatus status) {
        return switch (status) {
            case STARTED -> SagaState.SagaStatus.STARTED;
            case COMPLETED -> SagaState.SagaStatus.COMPLETED;
            case FAILED -> SagaState.SagaStatus.FAILED;
            case COMPENSATED -> SagaState.SagaStatus.COMPENSATED;
            case COMPENSATING -> SagaState.SagaStatus.FAILED;
            case COMPENSATION_FAILED -> SagaState.SagaStatus.FAILED;
        };
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public record CreateItem(UUID productId, int quantity) {}
    public record ReservedStock(UUID productId, int quantity) {}
}