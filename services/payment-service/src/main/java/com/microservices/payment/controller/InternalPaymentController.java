package com.microservices.payment.controller;

import com.microservices.payment.dto.request.ChargeRequest;
import com.microservices.payment.dto.request.RefundRequest;
import com.microservices.payment.dto.response.PaymentResponse;
import com.microservices.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/payments")
@RequiredArgsConstructor
@Hidden
public class InternalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public PaymentResponse charge(@Valid @RequestBody ChargeRequest request) {
        return paymentService.charge(request);
    }

    @PostMapping("/refund")
    public PaymentResponse refund(@Valid @RequestBody RefundRequest request) {
        return paymentService.refund(request);
    }
}