-- ============================================================
-- V1 : schéma initial du order-service (avec Saga)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------
-- Table orders
-- ------------------------------------------------------------
CREATE TABLE orders (
                        id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        order_number    VARCHAR(30) NOT NULL UNIQUE,
                        user_id         UUID NOT NULL,
                        status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        total_amount    NUMERIC(19, 2) NOT NULL,
                        currency        VARCHAR(3) NOT NULL DEFAULT 'EUR',
                        shipping_address TEXT,
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- ------------------------------------------------------------
-- Table order_items
-- ------------------------------------------------------------
CREATE TABLE order_items (
                             id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id      UUID NOT NULL,
                             product_sku     VARCHAR(50) NOT NULL,
                             product_name    VARCHAR(255) NOT NULL,
                             quantity        INTEGER NOT NULL CHECK (quantity > 0),
                             unit_price      NUMERIC(19, 2) NOT NULL CHECK (unit_price >= 0),
                             total_price     NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- ------------------------------------------------------------
-- Table saga_states (persistance des sagas)
-- ------------------------------------------------------------
CREATE TABLE saga_states (
                             id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             order_id        UUID NOT NULL,
                             step            VARCHAR(50) NOT NULL,
                             status          VARCHAR(20) NOT NULL,
                             payload         TEXT,
                             error_message   TEXT,
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_states_order_id ON saga_states(order_id);
CREATE INDEX idx_saga_states_status ON saga_states(status);

-- ------------------------------------------------------------
-- Table idempotency_keys
-- ------------------------------------------------------------
CREATE TABLE idempotency_keys (
                                  key             VARCHAR(255) PRIMARY KEY,
                                  response_body   TEXT,
                                  http_status     INTEGER,
                                  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys(created_at DESC);