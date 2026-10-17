-- ============================================================
-- V1 : schéma initial du product-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------
-- Catégories (hiérarchiques)
-- ------------------------------------------------------------
CREATE TABLE categories (
                            id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            name        VARCHAR(150) NOT NULL,
                            slug        VARCHAR(150) NOT NULL UNIQUE,
                            description TEXT,
                            parent_id   UUID REFERENCES categories(id) ON DELETE SET NULL,
                            created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_parent ON categories(parent_id);

-- ------------------------------------------------------------
-- Produits
-- ------------------------------------------------------------
CREATE TABLE products (
                          id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          sku          VARCHAR(50) NOT NULL UNIQUE,
                          name         VARCHAR(255) NOT NULL,
                          description  TEXT,
                          price        NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
                          currency     VARCHAR(3) NOT NULL DEFAULT 'EUR',
                          category_id  UUID REFERENCES categories(id) ON DELETE SET NULL,
                          image_url    VARCHAR(500),
                          active       BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          version      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_name ON products(name);

-- ------------------------------------------------------------
-- Seed : catégories de base
-- ------------------------------------------------------------
INSERT INTO categories (name, slug, description) VALUES
                                                     ('Électronique', 'electronique', 'Appareils électroniques et accessoires'),
                                                     ('Vêtements', 'vetements', 'Habillement homme, femme, enfant'),
                                                     ('Livres', 'livres', 'Livres, ebooks et bandes dessinées'),
                                                     ('Maison', 'maison', 'Meubles, décoration et ustensiles');

-- ------------------------------------------------------------
-- Seed : produits de démo
-- ------------------------------------------------------------
INSERT INTO products (sku, name, description, price, currency, category_id, active)
SELECT
    'PROD-001',
    'Smartphone XYZ Pro',
    'Smartphone haut de gamme avec écran OLED',
    899.99,
    'EUR',
    (SELECT id FROM categories WHERE slug = 'electronique'),
    TRUE;

INSERT INTO products (sku, name, description, price, currency, category_id, active)
SELECT
    'PROD-002',
    'Casque Audio BT',
    'Casque Bluetooth avec réduction de bruit',
    199.50,
    'EUR',
    (SELECT id FROM categories WHERE slug = 'electronique'),
    TRUE;

INSERT INTO products (sku, name, description, price, currency, category_id, active)
SELECT
    'PROD-003',
    'T-shirt Coton Bio',
    'T-shirt en coton biologique, col rond',
    29.90,
    'EUR',
    (SELECT id FROM categories WHERE slug = 'vetements'),
    TRUE;