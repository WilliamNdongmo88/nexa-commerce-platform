-- ============================================================
-- V1 : schéma initial du notification-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE notifications (
                               id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               type            VARCHAR(50) NOT NULL,             -- ORDER_CONFIRMED, PAYMENT_COMPLETED...
                               recipient_email VARCHAR(255),
                               recipient_id    UUID,
                               subject         VARCHAR(255) NOT NULL,
                               body            TEXT NOT NULL,
                               channel         VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
                               status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               reference_id    UUID,                             -- orderId, paymentId, etc.
                               reference_type  VARCHAR(30),                      -- ORDER, PAYMENT, USER
                               event_id        UUID,                             -- idempotency (eviter doublons)
                               sent_at         TIMESTAMPTZ,
                               error_message   TEXT,
                               created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_email);
CREATE INDEX idx_notifications_reference ON notifications(reference_id);
CREATE UNIQUE INDEX idx_notifications_event_id ON notifications(event_id) WHERE event_id IS NOT NULL;