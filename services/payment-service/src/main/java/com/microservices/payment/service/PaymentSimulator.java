package com.microservices.payment.service;

import com.microservices.payment.exception.PaymentFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulateur de paiement.
 * Règles :
 *  - Si amount > maxAmount → échec "amount_limit_exceeded"
 *  - Si cardNumber se termine par "0000" → échec "card_declined"
 *  - Si cardNumber se termine par "9999" → succès garanti
 *  - Sinon → succès (sauf si failureRate > 0)
 */
@Slf4j
@Component
public class PaymentSimulator {

    @Value("${app.payment.simulator.max-amount:10000.00}")
    private BigDecimal maxAmount;

    @Value("${app.payment.simulator.failure-rate:0.0}")
    private double failureRate;

    /**
     * Simule le paiement. Lève PaymentFailedException si refusé.
     * @return transaction ID (unique)
     */
    public String charge(BigDecimal amount, String cardNumber) {
        log.info("Simulating charge: amount={}, card={}", amount, maskCard(cardNumber));

        // Règle 1 : montant max
        if (amount.compareTo(maxAmount) > 0) {
            throw new PaymentFailedException(
                    "amount_limit_exceeded",
                    "Amount exceeds maximum allowed: " + maxAmount);
        }

        // Règle 2 : carte se terminant par 0000 → refus
        if (cardNumber != null && cardNumber.endsWith("0000")) {
            throw new PaymentFailedException(
                    "card_declined",
                    "Card declined by issuer");
        }

        // Règle 3 : carte se terminant par 9999 → succès garanti
        if (cardNumber != null && cardNumber.endsWith("9999")) {
            log.info("VIP card detected, always succeed");
            return generateTransactionId();
        }

        // Règle 4 : taux d'échec aléatoire (0 par défaut)
        if (failureRate > 0 && Math.random() < failureRate) {
            throw new PaymentFailedException(
                    "random_failure",
                    "Random failure for testing");
        }

        return generateTransactionId();
    }

    private String generateTransactionId() {
        return "txn_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String maskCard(String card) {
        if (card == null || card.length() < 4) return "****";
        return "****" + card.substring(card.length() - 4);
    }
}