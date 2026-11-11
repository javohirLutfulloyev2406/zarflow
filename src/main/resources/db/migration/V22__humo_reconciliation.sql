-- Humo balance-message reconciliation (docs/PRD.md §7).

-- A pending reconciliation offered to the user via bot buttons: either a linked card's
-- balance mismatch (account_id set, offer Tuzatish/O'tkazib yuborish) or an unlinked
-- card with a nonzero balance (account_id null, offer Hisobga bog'lash/E'tiborsiz
-- qoldirish). Deleted once acted on -- same lifecycle as telegram_pending_entries.
CREATE TABLE humo_reconciliation_drafts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    account_id   UUID REFERENCES accounts (id) ON DELETE RESTRICT,
    card_last4   VARCHAR(4) NOT NULL,
    bank_balance NUMERIC(19, 2) NOT NULL,
    as_of        TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Idempotency for "the same balance message forwarded twice" (same user, same original
-- message timestamp, same parsed card/balance content) -- second time replies
-- "allaqachon tekshirilgan" instead of reprocessing.
CREATE TABLE humo_balance_messages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    as_of         TIMESTAMPTZ NOT NULL,
    content_hash  VARCHAR(64) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_humo_balance_messages_dedupe ON humo_balance_messages (user_id, as_of, content_hash);

-- An unlinked card the user explicitly chose "E'tiborsiz qoldirish" for -- stops future
-- balance messages from re-offering Hisobga bog'lash/E'tiborsiz qoldirish for it.
CREATE TABLE humo_ignored_cards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    card_last4  VARCHAR(4) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_humo_ignored_cards_user_card ON humo_ignored_cards (user_id, card_last4);

-- Presence of a row means this user has already seen the one-time hint shown before the
-- first Tuzatish offer ("Avval shu kartaning Humo xabarlarini forward qiling...").
CREATE TABLE humo_hints (
    user_id   UUID PRIMARY KEY REFERENCES users (id) ON DELETE RESTRICT,
    shown_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);