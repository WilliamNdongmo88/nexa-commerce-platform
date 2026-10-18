-- ============================================================
-- V1 : schéma initial du payment-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------
-- Table payments
-- ------------------------------------------------------------
CREATE TABLE payments (
                          id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          order_id        UUID NOT NULL,
                          user_id         UUID NOT NULL,
                          amount          NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
                          currency        VARCHAR(3) NOT NULL DEFAULT 'EUR',
                          status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                          payment_method  VARCHAR(50) NOT NULL DEFAULT 'SIMULATED',
                          transaction_id  VARCHAR(100),                 -- ID unique de la transaction (fourni par le PSP)
                          error_code      VARCHAR(50),
                          error_message   VARCHAR(500),
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);

-- ------------------------------------------------------------
-- Table refunds (remboursements)
-- ------------------------------------------------------------
CREATE TABLE refunds (
                         id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         payment_id      UUID NOT NULL REFERENCES payments(id),
                         amount          NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
                         reason          VARCHAR(500),
                         status          VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
                         created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);