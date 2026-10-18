package com.microservices.payment.controller;

import com.microservices.payment.dto.response.PaymentResponse;
import com.microservices.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Consultation des paiements")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    @Operation(summary = "Récupère un paiement par ID")
    public PaymentResponse getById(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Récupère le paiement d'une commande")
    public PaymentResponse getByOrderId(@PathVariable UUID orderId) {
        return paymentService.getByOrderId(orderId);
    }
}