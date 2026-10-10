-- ============================================================
-- V1 : schéma initial du auth-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------
-- Table users
-- ------------------------------------------------------------
CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       first_name      VARCHAR(100),
                       last_name       VARCHAR(100),
                       enabled         BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_email ON users(email);

-- ------------------------------------------------------------
-- Table roles
-- ------------------------------------------------------------
CREATE TABLE roles (
                       id      BIGSERIAL PRIMARY KEY,
                       name    VARCHAR(50) NOT NULL UNIQUE
);

-- ------------------------------------------------------------
-- Table user_roles (n-n)
-- ------------------------------------------------------------
CREATE TABLE user_roles (
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

-- ------------------------------------------------------------
-- Table refresh_tokens
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
                                id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                token       VARCHAR(255) NOT NULL UNIQUE,
                                user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                expires_at  TIMESTAMPTZ NOT NULL,
                                revoked     BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ------------------------------------------------------------
-- Seed : rôles par défaut
-- ------------------------------------------------------------
INSERT INTO roles (name) VALUES
                             ('ROLE_ADMIN'),
                             ('ROLE_CUSTOMER'),
                             ('ROLE_SUPPORT'),
                             ('ROLE_SERVICE');