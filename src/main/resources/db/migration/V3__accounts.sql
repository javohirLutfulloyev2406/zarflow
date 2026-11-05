CREATE TABLE accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name             VARCHAR(60) NOT NULL,
    type             VARCHAR(20) NOT NULL CHECK (type IN ('CASH', 'BANK_CARD', 'BANK_ACCOUNT', 'SAVINGS', 'OTHER')),
    currency         VARCHAR(3) NOT NULL DEFAULT 'UZS' CHECK (currency = 'UZS'),
    initial_balance  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    color            VARCHAR(7),
    is_default       BOOLEAN NOT NULL DEFAULT false,
    version          BIGINT NOT NULL DEFAULT 0,
    archived_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_accounts_user_id ON accounts (user_id);

CREATE UNIQUE INDEX ux_accounts_default_per_user ON accounts (user_id) WHERE is_default AND archived_at IS NULL;
