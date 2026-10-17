package com.microservices.product.repository;

import com.microservices.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    // ============================================================
    // 4 combinaisons possibles (filtres optionnels)
    // ============================================================

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByActiveTrueAndCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByActiveTrueAndCategoryIdAndNameContainingIgnoreCase(
            UUID categoryId, String name, Pageable pageable);
}