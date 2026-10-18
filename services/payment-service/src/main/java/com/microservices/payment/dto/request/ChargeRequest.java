package com.microservices.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeRequest(
        @NotNull UUID orderId,
        @NotNull UUID userId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        // Optionnel : carte de test pour simuler l'échec
        String cardNumber
) {}