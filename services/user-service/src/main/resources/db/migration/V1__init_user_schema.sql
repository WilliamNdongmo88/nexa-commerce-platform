-- ============================================================
-- V1 : schéma initial du user-service
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------
-- Table user_profiles
-- La PK est l'ID utilisateur (fourni par auth-service)
-- ------------------------------------------------------------
CREATE TABLE user_profiles (
                               id              UUID PRIMARY KEY,             -- même ID que dans auth_db.users
                               email           VARCHAR(255) NOT NULL UNIQUE,
                               first_name      VARCHAR(100) NOT NULL,
                               last_name       VARCHAR(100) NOT NULL,
                               phone           VARCHAR(30),
                               birth_date      DATE,
                               address_line1   VARCHAR(255),
                               address_line2   VARCHAR(255),
                               city            VARCHAR(100),
                               postal_code     VARCHAR(20),
                               country         VARCHAR(100),
                               avatar_url      VARCHAR(500),
                               bio             TEXT,
                               locale          VARCHAR(10) DEFAULT 'fr',
                               timezone        VARCHAR(50) DEFAULT 'UTC',
                               created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_user_profiles_country ON user_profiles(country);