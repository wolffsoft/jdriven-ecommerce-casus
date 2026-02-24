CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS products (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "name"           VARCHAR(255) NOT NULL,
    description      TEXT,
    price_in_cents   BIGINT NOT NULL CHECK (price_in_cents >= 0),
    currency         VARCHAR(10) NOT NULL,
    attributes       JSONB NOT NULL DEFAULT '{}'::jsonb,
    price_updated_at TIMESTAMPTZ NULL
    );

ALTER TABLE products ADD CONSTRAINT chk_products_currency_len CHECK (char_length(currency) >= 3);
