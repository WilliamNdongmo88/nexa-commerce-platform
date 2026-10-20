package com.microservices.product.service;

import com.microservices.product.dto.request.CreateProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.entity.Category;
import com.microservices.product.entity.Product;
import com.microservices.product.event.ProductEventPublisher;
import com.microservices.product.exception.SkuAlreadyExistsException;
import com.microservices.product.mapper.ProductMapper;
import com.microservices.product.repository.CategoryRepository;
import com.microservices.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductEventPublisher productEventPublisher;   // ⭐ NOUVEAU : à ajouter

    @InjectMocks
    private ProductService productService;

    private CreateProductRequest validRequest;
    private Product productEntity;
    private ProductResponse productResponse;
    private Category category;

    @BeforeEach
    void setUp() {
        UUID categoryId = UUID.randomUUID();
        category = Category.builder().id(categoryId).name("Electronique").build();

        validRequest = new CreateProductRequest(
                "PROD-123", "Smartphone", "Super tel",
                new BigDecimal("999.99"), "EUR", categoryId, "http://img.com"
        );

        productEntity = Product.builder()
                .id(UUID.randomUUID())
                .sku("PROD-123")
                .name("Smartphone")
                .price(new BigDecimal("999.99"))
                .currency("EUR")
                .category(category)
                .active(true)
                .build();

        productResponse = new ProductResponse(
                productEntity.getId(), "PROD-123", "Smartphone", "Super tel",
                new BigDecimal("999.99"), "EUR", null, null, true, null, null
        );
    }

    @Test
    void createProduct_ShouldSucceed_WhenSkuIsUnique() {
        // Arrange
        when(productRepository.existsBySku("PROD-123")).thenReturn(false);
        when(categoryRepository.findById(any())).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(productEntity);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        // Act
        ProductResponse result = productService.create(validRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.sku()).isEqualTo("PROD-123");
        assertThat(result.price()).isEqualTo(new BigDecimal("999.99"));

        // Vérifie que le repository a bien été appelé
        verify(productRepository, times(1)).save(any(Product.class));
        // ⭐ Vérifie que l'événement a bien été publié
        verify(productEventPublisher, times(1))
                .publishProductCreated(any(Product.class), anyInt());
    }

    @Test
    void createProduct_ShouldThrowException_WhenSkuExists() {
        // Arrange
        when(productRepository.existsBySku("PROD-123")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.create(validRequest))
                .isInstanceOf(SkuAlreadyExistsException.class)
                .hasMessageContaining("PROD-123");

        // Vérifie que le produit n'a PAS été sauvegardé
        verify(productRepository, never()).save(any(Product.class));
        // ⭐ Vérifie que l'événement n'a PAS été publié
        verify(productEventPublisher, never())
                .publishProductCreated(any(Product.class), anyInt());
    }
}