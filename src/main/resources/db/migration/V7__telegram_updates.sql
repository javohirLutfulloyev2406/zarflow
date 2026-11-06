CREATE TABLE telegram_updates (
    update_id   BIGINT PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
