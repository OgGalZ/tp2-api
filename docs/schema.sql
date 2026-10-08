-- Structure de la base générée par Hibernate (ddl-auto=update) sur PostgreSQL 16.
-- Fournie pour le livrable « structure de la base de données ».

-- ===== Exercice 1 : Calculatrice =====
CREATE TABLE operations (
    id         BIGSERIAL PRIMARY KEY,
    operateur  VARCHAR(20)      NOT NULL CHECK (operateur IN ('ADDITION','SOUSTRACTION','MULTIPLICATION','DIVISION')),
    a          DOUBLE PRECISION NOT NULL,
    b          DOUBLE PRECISION NOT NULL,
    resultat   DOUBLE PRECISION NOT NULL,
    date       TIMESTAMP(6)     NOT NULL
);

-- ===== Exercice 2 : Devises =====
CREATE TABLE exchange_rates (
    id              BIGSERIAL PRIMARY KEY,
    base_currency   VARCHAR(3)     NOT NULL,
    target_currency VARCHAR(3)     NOT NULL,
    rate            NUMERIC(19, 8) NOT NULL,
    updated_at      TIMESTAMP(6)   NOT NULL,
    CONSTRAINT uk_exchange_rates_pair UNIQUE (base_currency, target_currency)

);

CREATE TABLE currency_transactions (
    id               BIGSERIAL PRIMARY KEY,
    amount           NUMERIC(19, 4) NOT NULL,
    from_currency    VARCHAR(3)     NOT NULL,
    to_currency      VARCHAR(3)     NOT NULL,
    rate             NUMERIC(19, 8) NOT NULL,
    converted_amount NUMERIC(19, 4) NOT NULL,
    source           VARCHAR(10)    NOT NULL CHECK (source IN ('API','LOCAL','MANUAL')),
    date             TIMESTAMP(6)   NOT NULL
);

-- ===== Exercice 3 : Utilisateurs, refresh tokens, BMI =====
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,          -- hash BCrypt, jamais le mot de passe en clair
    email      VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(100) NOT NULL UNIQUE,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN      NOT NULL
);

CREATE TABLE bmi_records (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT           NOT NULL,
    weight     DOUBLE PRECISION NOT NULL,     -- kg
    height     DOUBLE PRECISION NOT NULL,     -- m
    bmi        DOUBLE PRECISION NOT NULL,
    category   VARCHAR(30)      NOT NULL CHECK (category IN ('INSUFFISANCE_PONDERALE','NORMAL','SURPOIDS','OBESITE')),
    created_at TIMESTAMP(6)     NOT NULL,
    updated_at TIMESTAMP(6)
);
CREATE INDEX idx_bmi_user ON bmi_records (user_id);
