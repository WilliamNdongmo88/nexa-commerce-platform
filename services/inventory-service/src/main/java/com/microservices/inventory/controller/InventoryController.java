package com.microservices.inventory.controller;

import com.microservices.inventory.dto.request.AdjustStockRequest;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.entity.StockMovement;
import com.microservices.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Gestion des stocks")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Consulte le stock d'un produit")
    public InventoryResponse getByProduct(@PathVariable UUID productId) {
        return inventoryService.getByProductId(productId);
    }

    @GetMapping("/{productId}/movements")
    @Operation(summary = "Historique des mouvements de stock d'un produit")
    public List<StockMovement> getMovements(@PathVariable UUID productId) {
        return inventoryService.getMovements(productId);
    }

    @PostMapping("/adjust")
    @Operation(summary = "Ajuste manuellement le stock (ROLE_ADMIN)")
    public InventoryResponse adjust(@Valid @RequestBody AdjustStockRequest request) {
        return inventoryService.adjust(request);
    }
}