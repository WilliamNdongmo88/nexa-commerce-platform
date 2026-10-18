package com.microservices.payment.service;

import com.microservices.payment.dto.request.ChargeRequest;
import com.microservices.payment.dto.request.RefundRequest;
import com.microservices.payment.dto.response.PaymentResponse;
import com.microservices.payment.entity.Payment;
import com.microservices.payment.entity.Refund;
import com.microservices.payment.event.PaymentEventPublisher;
import com.microservices.payment.exception.PaymentFailedException;
import com.microservices.payment.exception.PaymentNotFoundException;
import com.microservices.payment.mapper.PaymentMapper;
import com.microservices.payment.repository.PaymentRepository;
import com.microservices.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentSimulator simulator;
    private final PaymentMapper mapper;
    private final PaymentEventPublisher eventPublisher;

    // ============================================================
    // CHARGE — débiter
    // ============================================================
    @Transactional
    public PaymentResponse charge(ChargeRequest request) {
        // Idempotency : si un paiement existe déjà pour cette commande
        var existing = paymentRepository.findByOrderId(request.orderId());
        if (existing.isPresent() && existing.get().getStatus() == Payment.PaymentStatus.COMPLETED) {
            log.info("Payment already exists for order {}, returning existing", request.orderId());
            return mapper.toResponse(existing.get());
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod("SIMULATED")
                .build();

        try {
            String transactionId = simulator.charge(request.amount(), request.cardNumber());
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setTransactionId(transactionId);
            paymentRepository.save(payment);
            eventPublisher.publishCompleted(payment);
            log.info("Payment completed: {} (txn={})", payment.getId(), transactionId);
            return mapper.toResponse(payment);

        } catch (PaymentFailedException e) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setErrorCode(e.getErrorCode());
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);
            eventPublisher.publishCompleted(payment);
            log.warn("Payment failed: {} ({})", payment.getId(), e.getErrorCode());
            throw e;
        }
    }

    // ============================================================
    // REFUND — rembourser
    // ============================================================
    @Transactional
    public PaymentResponse refund(RefundRequest request) {
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(request.paymentId()));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot refund payment in status " + payment.getStatus());
        }

        if (request.amount().compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount exceeds original payment amount");
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(request.amount())
                .reason(request.reason())
                .status("COMPLETED")
                .build();
        refundRepository.save(refund);

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);

        log.info("Refund processed: payment={}, amount={}", payment.getId(), request.amount());
        return mapper.toResponse(saved);
    }

    // ============================================================
    // READ
    // ============================================================
    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return mapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
        return mapper.toResponse(payment);
    }
}