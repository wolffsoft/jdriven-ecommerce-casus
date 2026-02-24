CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS outbox_event (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id         VARCHAR(128) NOT NULL,
    event_type           VARCHAR(128) NOT NULL,
    payload              TEXT NOT NULL,
    status               VARCHAR(32) NOT NULL DEFAULT 'NEW',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at         TIMESTAMPTZ NULL,
    publish_attempts     INT NOT NULL DEFAULT 0,
    next_attempt_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_by            VARCHAR(128) NULL,
    locked_at            TIMESTAMPTZ NULL,
    last_error           VARCHAR(4000) NULL
    );

-- Efficient polling for publisher (ready rows)
CREATE INDEX IF NOT EXISTS idx_outbox_ready ON outbox_event (status, next_attempt_at, created_at);
