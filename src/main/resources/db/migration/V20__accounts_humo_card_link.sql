-- Links a Humo card (last 4 digits, forwarded from @HUMOcardbot balance messages) to the
-- account whose balance it should reconcile against (docs/PRD.md §7 Humo reconciliation).
ALTER TABLE accounts ADD COLUMN humo_card_last4 VARCHAR(4);

CREATE UNIQUE INDEX ux_accounts_humo_card_last4
    ON accounts (user_id, humo_card_last4)
    WHERE humo_card_last4 IS NOT NULL AND archived_at IS NULL;