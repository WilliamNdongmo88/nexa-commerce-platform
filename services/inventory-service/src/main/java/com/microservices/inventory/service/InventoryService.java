package com.microservices.inventory.service;

import com.microservices.inventory.dto.request.AdjustStockRequest;
import com.microservices.inventory.dto.request.ReleaseStockRequest;
import com.microservices.inventory.dto.request.ReserveStockRequest;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.entity.Inventory;
import com.microservices.inventory.entity.StockMovement;
import com.microservices.inventory.event.InventoryEventPublisher;
import com.microservices.inventory.exception.InsufficientStockException;
import com.microservices.inventory.exception.InventoryNotFoundException;
import com.microservices.inventory.mapper.InventoryMapper;
import com.microservices.inventory.repository.InventoryRepository;
import com.microservices.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryEventPublisher eventPublisher;

    // ============================================================
    // READ
    // ============================================================
    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        return inventoryMapper.toResponse(inv);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getMovements(UUID productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    // ============================================================
    // CREATE (à appeler quand un produit est créé)
    // ============================================================
    @Transactional
    public Inventory createInventory(UUID productId, String sku, int initialQuantity) {
        if (inventoryRepository.existsByProductId(productId)) {
            log.warn("Inventory already exists for product {}, skipping", productId);
            return inventoryRepository.findByProductId(productId).orElseThrow();
        }

        Inventory inv = Inventory.builder()
                .productId(productId)
                .productSku(sku)
                .quantity(initialQuantity)
                .reserved(0)
                .lowStockThreshold(5)
                .build();

        Inventory saved = inventoryRepository.save(inv);
        recordMovement(productId, StockMovement.MovementType.INIT, initialQuantity, "Initial stock", null);
        log.info("Inventory created for product {} (sku={}, qty={})", productId, sku, initialQuantity);
        return saved;
    }

    // ============================================================
    // RESERVE — appelé lors d'une création de commande
    // ============================================================
    @Transactional
    public InventoryResponse reserve(ReserveStockRequest request) {
        Inventory inv = inventoryRepository
                .findByProductIdForUpdate(request.productId())    // verrou pessimiste
                .orElseThrow(() -> new InventoryNotFoundException(request.productId()));

        int available = inv.getAvailable();
        if (available < request.quantity()) {
            throw new InsufficientStockException(request.productId(), request.quantity(), available);
        }

        inv.setReserved(inv.getReserved() + request.quantity());
        Inventory saved = inventoryRepository.save(inv);

        recordMovement(
                request.productId(),
                StockMovement.MovementType.RESERVE,
                request.quantity(),
                "Reserved for order",
                request.orderId()
        );

        log.info("Reserved {} of product {} (orderId={})", request.quantity(), request.productId(), request.orderId());
        eventPublisher.publishStockReserved(request.productId(), request.quantity(), request.orderId());
        return inventoryMapper.toResponse(saved);
    }

    // ============================================================
    // RELEASE — libérer (rollback de réservation)
    // ============================================================
    @Transactional
    public InventoryResponse release(ReleaseStockRequest request) {
        Inventory inv = inventoryRepository
                .findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(request.productId()));

        if (inv.getReserved() < request.quantity()) {
            throw new IllegalArgumentException(
                    "Cannot release " + request.quantity() + " — only " + inv.getReserved() + " reserved");
        }

        inv.setReserved(inv.getReserved() - request.quantity());
        Inventory saved = inventoryRepository.save(inv);

        recordMovement(
                request.productId(),
                StockMovement.MovementType.RELEASE,
                request.quantity(),
                "Released reservation",
                request.orderId()
        );

        log.info("Released {} of product {} (orderId={})", request.quantity(), request.productId(), request.orderId());
        eventPublisher.publishStockReleased(request.productId(), request.quantity(), request.orderId());
        return inventoryMapper.toResponse(saved);
    }

    // ============================================================
    // COMMIT — confirmer (paiement OK → déduire physiquement)
    // ============================================================
    @Transactional
    public InventoryResponse commit(UUID productId, int quantity, UUID orderId) {
        Inventory inv = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        if (inv.getReserved() < quantity) {
            throw new IllegalArgumentException(
                    "Cannot commit " + quantity + " — only " + inv.getReserved() + " reserved");
        }
        if (inv.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Cannot commit " + quantity + " — only " + inv.getQuantity() + " in stock");
        }

        inv.setReserved(inv.getReserved() - quantity);
        inv.setQuantity(inv.getQuantity() - quantity);
        Inventory saved = inventoryRepository.save(inv);

        recordMovement(productId, StockMovement.MovementType.COMMIT, quantity,
                "Committed after payment", orderId);

        log.info("Committed {} of product {} (orderId={})", quantity, productId, orderId);
        eventPublisher.publishStockReserved(productId, quantity, orderId);
        return inventoryMapper.toResponse(saved);
    }

    // ============================================================
    // ADJUST — ajustement manuel (admin : inventaire physique)
    // ============================================================
    @Transactional
    public InventoryResponse adjust(AdjustStockRequest request) {
        Inventory inv = inventoryRepository
                .findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(request.productId()));

        int newQty = inv.getQuantity() + request.quantityDelta();
        if (newQty < 0) {
            throw new IllegalArgumentException("Adjustment would result in negative stock");
        }

        inv.setQuantity(newQty);
        Inventory saved = inventoryRepository.save(inv);

        recordMovement(request.productId(), StockMovement.MovementType.ADJUST,
                request.quantityDelta(), request.reason(), null);

        log.info("Adjusted stock of product {} by {} (new qty={})",
                request.productId(), request.quantityDelta(), newQty);
        return inventoryMapper.toResponse(saved);
    }

    // ============================================================
    // Helper
    // ============================================================
    private void recordMovement(UUID productId, StockMovement.MovementType type,
                                int quantity, String reason, UUID orderId) {
        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .type(type)
                .quantity(quantity)
                .reason(reason)
                .orderId(orderId)
                .build();
        stockMovementRepository.save(movement);
    }
}