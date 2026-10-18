package com.microservices.product.service;

import com.microservices.product.dto.request.CreateProductRequest;
import com.microservices.product.dto.request.UpdateProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.entity.Category;
import com.microservices.product.entity.Product;
import com.microservices.product.event.ProductEventPublisher;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.exception.SkuAlreadyExistsException;
import com.microservices.product.mapper.ProductMapper;
import com.microservices.product.repository.CategoryRepository;
import com.microservices.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductEventPublisher productEventPublisher;

    // ============================================================
    // READ
    // ============================================================
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
        return productMapper.toResponse(product);
    }

    /**
     * Recherche avec filtres optionnels (categoryId, search).
     * On utilise 4 méthodes distinctes pour éviter le piège PostgreSQL
     * "lower(bytea) does not exist" quand le paramètre est null.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(UUID categoryId, String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();

        Page<Product> result;
        if (categoryId != null && normalizedSearch != null) {
            result = productRepository.findByActiveTrueAndCategoryIdAndNameContainingIgnoreCase(
                    categoryId, normalizedSearch, pageable);
        } else if (categoryId != null) {
            result = productRepository.findByActiveTrueAndCategoryId(categoryId, pageable);
        } else if (normalizedSearch != null) {
            result = productRepository.findByActiveTrueAndNameContainingIgnoreCase(
                    normalizedSearch, pageable);
        } else {
            result = productRepository.findByActiveTrue(pageable);
        }

        return result.map(productMapper::toResponse);
    }

    // ============================================================
    // CREATE
    // ============================================================
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new SkuAlreadyExistsException(request.sku());
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Category not found: " + request.categoryId()));
        }

        Product product = Product.builder()
                .sku(request.sku().toUpperCase().trim())
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .currency(request.currency().toUpperCase())
                .category(category)
                .imageUrl(request.imageUrl())
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: {} ({})", saved.getSku(), saved.getId());

        productEventPublisher.publishProductCreated(saved, 0);

        return productMapper.toResponse(saved);
    }

    // ============================================================
    // UPDATE
    // ============================================================
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (request.name() != null) product.setName(request.name().trim());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.currency() != null) product.setCurrency(request.currency().toUpperCase());
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.active() != null) product.setActive(request.active());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Category not found: " + request.categoryId()));
            product.setCategory(category);
        }

        Product updated = productRepository.save(product);
        log.info("Product updated: {}", id);
        return productMapper.toResponse(updated);
    }

    // ============================================================
    // DELETE (soft)
    // ============================================================
    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setActive(false);   // soft delete
        productRepository.save(product);
        log.info("Product soft-deleted: {}", id);
    }
}