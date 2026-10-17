package com.microservices.product.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // 404 - Produit non trouvé
    // ============================================================
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex, HttpServletRequest req) {
        log.warn("ProductNotFoundException on {}: {}", req.getRequestURI(), ex.getMessage());
        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Product not found",
                ex.getMessage(),
                req
        );
    }

    // ============================================================
    // 409 - SKU déjà existant
    // ============================================================
    @ExceptionHandler(SkuAlreadyExistsException.class)
    public ProblemDetail handleSkuAlreadyExists(SkuAlreadyExistsException ex, HttpServletRequest req) {
        log.warn("SkuAlreadyExistsException on {}: {}", req.getRequestURI(), ex.getMessage());
        return buildProblem(
                HttpStatus.CONFLICT,
                "SKU already exists",
                ex.getMessage(),
                req
        );
    }

    // ============================================================
    // 400 - Validation (Bean Validation sur @RequestBody)
    // ============================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        log.warn("Validation failed on {}: {}", req.getRequestURI(), ex.getMessage());

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "Invalid request payload",
                req
        );

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    // ============================================================
    // 400 - Argument invalide (UUID malformé, catégorie inexistante, etc.)
    // ============================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("IllegalArgumentException on {}: {}", req.getRequestURI(), ex.getMessage());
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid argument",
                ex.getMessage(),
                req
        );
    }

    // ============================================================
    // 500 - Fallback générique
    // ============================================================
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal error",
                "An unexpected error occurred. Please contact support.",
                req
        );
    }

    // ============================================================
    // Helper : construit un ProblemDetail RFC 7807
    // ============================================================
    private ProblemDetail buildProblem(HttpStatus status,
                                       String title,
                                       String detail,
                                       HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://api.example.com/errors/" + status.value()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", req.getRequestURI());
        return problem;
    }

    // ============================================================
    // DTO interne : erreur de validation
    // ============================================================
    public record ValidationError(String field, String message) {}
}