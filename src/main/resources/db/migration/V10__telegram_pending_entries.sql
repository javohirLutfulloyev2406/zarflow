-- A quick-input line the parser couldn't confidently categorize: held here, unsaved,
-- until the user taps a category button (see QuickInputParser.Confidence.LOW).
CREATE TABLE telegram_pending_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    account_id  UUID NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    type        VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount      NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    description VARCHAR(280),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
