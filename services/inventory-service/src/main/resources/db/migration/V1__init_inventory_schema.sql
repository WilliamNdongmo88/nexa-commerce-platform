-- ============================================================
-- V1 : schéma initial du inventory-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------
-- Table inventory (1 ligne par produit)
-- ------------------------------------------------------------
CREATE TABLE inventories (
                             id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             product_id      UUID NOT NULL UNIQUE,      -- ID du produit (product_db), pas de FK cross-service
                             product_sku     VARCHAR(50) NOT NULL,      -- dénormalisé pour debug
                             quantity        INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                             reserved        INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0),
                             low_stock_threshold INTEGER NOT NULL DEFAULT 5,
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_inventories_product_id ON inventories(product_id);
CREATE INDEX idx_inventories_sku ON inventories(product_sku);

-- Contrainte : reserved <= quantity
ALTER TABLE inventories
    ADD CONSTRAINT chk_reserved_le_quantity CHECK (reserved <= quantity);

-- ------------------------------------------------------------
-- Table stock_movements (historique des mouvements)
-- ------------------------------------------------------------
CREATE TABLE stock_movements (
                                 id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 product_id      UUID NOT NULL,
                                 type            VARCHAR(30) NOT NULL,      -- RESERVE, RELEASE, COMMIT, ADJUST, INIT
                                 quantity        INTEGER NOT NULL,
                                 reason          VARCHAR(255),
                                 order_id        UUID,
                                 created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_order_id ON stock_movements(order_id);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at DESC);