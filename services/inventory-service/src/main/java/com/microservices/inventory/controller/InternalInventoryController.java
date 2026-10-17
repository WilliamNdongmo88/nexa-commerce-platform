package com.microservices.inventory.controller;

import com.microservices.inventory.dto.request.ReleaseStockRequest;
import com.microservices.inventory.dto.request.ReserveStockRequest;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints internes — non exposés par la Gateway.
 * Utilisés par order-service (via WebClient) pour réserver/libérer le stock.
 */
@RestController
@RequestMapping("/internal/v1/inventory")
@RequiredArgsConstructor
@Hidden
public class InternalInventoryController {

    private final InventoryService inventoryService;

    public record CommitRequest(UUID productId, int quantity, UUID orderId) {}

    @PostMapping("/reserve")
    public InventoryResponse reserve(@Valid @RequestBody ReserveStockRequest request) {
        return inventoryService.reserve(request);
    }

    @PostMapping("/release")
    public InventoryResponse release(@Valid @RequestBody ReleaseStockRequest request) {
        return inventoryService.release(request);
    }

    @PostMapping("/commit")
    public InventoryResponse commit(@RequestBody CommitRequest request) {
        return inventoryService.commit(request.productId(), request.quantity(), request.orderId());
    }

    /**
     * Endpoint interne utilisé par product-service (ou manuellement)
     * pour créer un stock initial quand un produit est créé.
     */
    public record CreateInventoryRequest(UUID productId, String sku, int initialQuantity) {}

    @PostMapping("/create")
    public InventoryResponse create(@RequestBody CreateInventoryRequest request) {
        var inv = inventoryService.createInventory(
                request.productId(), request.sku(), request.initialQuantity());
        return inventoryService.getByProductId(inv.getProductId());
    }
}