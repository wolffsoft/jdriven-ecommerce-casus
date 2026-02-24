CREATE TABLE IF NOT EXISTS price_update_inbox (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id       VARCHAR(128) NOT NULL,
    product_id       UUID NOT NULL,
    effective_at     TIMESTAMPTZ NOT NULL,
    received_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    "source"         VARCHAR(128),
    status           VARCHAR(32) NOT NULL DEFAULT 'NEW',
    process_attempts INT NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at     TIMESTAMPTZ NULL,
    last_error       VARCHAR(4000) NULL,

    CONSTRAINT catalog_price_update_inbox_request_id UNIQUE (request_id)
    );

CREATE INDEX IF NOT EXISTS ix_price_update_inbox_product_id ON price_update_inbox (product_id);
CREATE INDEX IF NOT EXISTS ix_price_update_inbox_received_at ON price_update_inbox (received_at);
CREATE INDEX IF NOT EXISTS ix_price_update_inbox_ready ON price_update_inbox (status, next_attempt_at, received_at);
