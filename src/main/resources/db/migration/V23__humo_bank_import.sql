-- HUMO card transaction import (docs/PRD.md §1-5).

-- Dedupe for imported transactions: fingerprint = sha256("HUMO|last4|sign|amount|
-- yyyy-MM-ddTHH:mm|merchantRaw"). transaction_id is nullable -- filled in once the row
-- is actually booked, left null if the user resolves the pending draft as "Dublikat".
CREATE TABLE bank_imports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    fingerprint     VARCHAR(64) NOT NULL,
    transaction_id  UUID REFERENCES transactions (id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_bank_imports_user_fingerprint ON bank_imports (user_id, fingerprint);

-- A pending question for one parsed HUMO transaction block, offered to the user via bot
-- buttons (card link, P2P classification/own-transfer account, loan debt pick, category,
-- possible-duplicate confirm). Deleted once fully resolved -- same lifecycle as
-- telegram_pending_entries / humo_reconciliation_drafts.
CREATE TABLE humo_import_drafts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    bank_import_id    UUID NOT NULL REFERENCES bank_imports (id) ON DELETE RESTRICT,
    type              VARCHAR(16) NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    merchant_raw      TEXT NOT NULL,
    merchant_key      TEXT NOT NULL,
    card_last4        VARCHAR(4) NOT NULL,
    account_id        UUID REFERENCES accounts (id) ON DELETE RESTRICT,
    category_id       UUID REFERENCES categories (id) ON DELETE RESTRICT,
    transaction_date  TIMESTAMPTZ NOT NULL,
    state             VARCHAR(32) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Remembers a user's P2P merchant-key classification (OWN_TRANSFER / EXPENSE / INCOME,
-- docs/PRD.md §4) -- not a category id, so kept separate from user_parsing_memory. Only
-- the classification is remembered; an OWN_TRANSFER still asks which destination account
-- every time.
CREATE TABLE humo_p2p_choices (
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    merchant_key  TEXT NOT NULL,
    choice        VARCHAR(16) NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, merchant_key)
);
