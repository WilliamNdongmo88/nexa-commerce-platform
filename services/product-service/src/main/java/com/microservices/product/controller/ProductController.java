package com.microservices.product.controller;

import com.microservices.product.dto.request.CreateProductRequest;
import com.microservices.product.dto.request.UpdateProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Catalogue produits")
public class ProductController {

    private final ProductService productService;

    // ============================================================
    // READ (public)
    // ============================================================
    @GetMapping
    @Operation(summary = "Liste paginée des produits (public)")
    public Page<ProductResponse> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        // Sécurisation du paramètre sort pour éviter les crashs
        String[] sortParams = sort.split(",");
        String sortField = (sortParams.length > 0) ? sortParams[0] : "createdAt";
        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return productService.search(categoryId, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un produit par ID (public)")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Récupère un produit par SKU (public)")
    public ProductResponse getBySku(@PathVariable String sku) {
        return productService.getBySku(sku);
    }

    // ============================================================
    // WRITE (ROLE_ADMIN — enforced by Gateway)
    // ============================================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un produit (ROLE_ADMIN)")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un produit (ROLE_ADMIN)")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactive un produit (ROLE_ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}