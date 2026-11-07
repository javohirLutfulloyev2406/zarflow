CREATE TABLE transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    account_id        UUID NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    category_id       UUID REFERENCES categories (id) ON DELETE RESTRICT,
    type              VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    -- Income/expense amounts are always a positive magnitude. A transfer's two legs
    -- share one transfer_group_id but are signed opposite -- negative on the source
    -- account's row, positive on the destination's -- so that balance = initial_balance
    -- + SUM(amount) works uniformly per account without a per-type CASE in that query.
    amount            NUMERIC(19, 2) NOT NULL,
    currency          VARCHAR(3) NOT NULL DEFAULT 'UZS' CHECK (currency = 'UZS'),
    necessity         VARCHAR(15) CHECK (necessity IN ('ESSENTIAL', 'DISCRETIONARY')),
    transaction_date  TIMESTAMPTZ NOT NULL,
    description       VARCHAR(280),
    source            VARCHAR(10) NOT NULL CHECK (source IN ('BOT', 'MINI_APP')),
    transfer_group_id UUID,
    idempotency_key   VARCHAR(100),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    CHECK ((type IN ('EXPENSE', 'INCOME') AND amount > 0) OR (type = 'TRANSFER' AND amount <> 0)),
    CHECK (type = 'EXPENSE' OR necessity IS NULL),
    CHECK (type <> 'TRANSFER' OR category_id IS NULL),
    CHECK (type = 'TRANSFER' OR transfer_group_id IS NULL)
);

CREATE INDEX ix_transactions_user_id_transaction_date ON transactions (user_id, transaction_date);
CREATE INDEX ix_transactions_account_id ON transactions (account_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_transactions_transfer_group_id ON transactions (transfer_group_id) WHERE transfer_group_id IS NOT NULL;
CREATE UNIQUE INDEX ux_transactions_user_idempotency_key ON transactions (user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE transaction_audit (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions (id) ON DELETE RESTRICT,
    action         VARCHAR(10) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    before         JSONB,
    after          JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_transaction_audit_transaction_id ON transaction_audit (transaction_id);
