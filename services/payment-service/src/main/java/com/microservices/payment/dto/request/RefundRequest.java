package com.microservices.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        @NotNull UUID paymentId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String reason
) {}