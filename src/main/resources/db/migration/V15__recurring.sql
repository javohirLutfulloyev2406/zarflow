CREATE TABLE recurring_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name                VARCHAR(60) NOT NULL,
    kind                VARCHAR(10) NOT NULL CHECK (kind IN ('REGULAR', 'LOAN', 'INCOME')),
    amount              NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    category_id         UUID REFERENCES categories (id) ON DELETE RESTRICT,
    account_id          UUID NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    schedule            VARCHAR(15) NOT NULL CHECK (schedule IN ('MONTHLY', 'WEEKLY', 'EVERY_N_DAYS')),
    day_of_month        INTEGER CHECK (day_of_month BETWEEN 1 AND 31),
    day_of_week         INTEGER CHECK (day_of_week BETWEEN 1 AND 7),
    every_n_days        INTEGER CHECK (every_n_days > 0),
    start_date          DATE NOT NULL,
    next_due_date       DATE NOT NULL,
    auto_create         BOOLEAN NOT NULL DEFAULT false,
    remind_days_before  INTEGER NOT NULL DEFAULT 1,
    active              BOOLEAN NOT NULL DEFAULT true,
    archived_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- LOAN rules book straight to DEBT_PAYMENT with no category; REGULAR/INCOME need one
    -- so the resulting transaction has somewhere to land.
    CHECK (kind = 'LOAN' OR category_id IS NOT NULL),
    CHECK (schedule <> 'MONTHLY' OR day_of_month IS NOT NULL),
    CHECK (schedule <> 'WEEKLY' OR day_of_week IS NOT NULL),
    CHECK (schedule <> 'EVERY_N_DAYS' OR every_n_days IS NOT NULL)
);

CREATE INDEX ix_recurring_rules_user_id ON recurring_rules (user_id) WHERE archived_at IS NULL;
CREATE INDEX ix_recurring_rules_due_scan ON recurring_rules (next_due_date) WHERE active AND archived_at IS NULL;

CREATE TABLE recurring_occurrences (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id              UUID NOT NULL REFERENCES recurring_rules (id) ON DELETE RESTRICT,
    due_date             DATE NOT NULL,
    status               VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'SKIPPED')),
    transaction_id       UUID REFERENCES transactions (id) ON DELETE RESTRICT,
    -- Not in the PRD's literal column list, but required: the scheduler ticks every 5
    -- minutes, and each of the two reminder points ("N days before" and "on the due day")
    -- must fire exactly once, not on every tick within that day.
    reminded_before_sent BOOLEAN NOT NULL DEFAULT false,
    reminded_due_sent    BOOLEAN NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_recurring_occurrences_rule_id_due_date ON recurring_occurrences (rule_id, due_date);

-- transactions.type gains DEBT_PAYMENT: a loan/credit installment -- reduces balance like
-- EXPENSE, but is excluded from expense analytics and the daily safe-to-spend limit.
ALTER TABLE transactions ALTER COLUMN type TYPE VARCHAR(15);

ALTER TABLE transactions DROP CONSTRAINT transactions_type_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_type_check
    CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'DEBT_PAYMENT'));

ALTER TABLE transactions DROP CONSTRAINT transactions_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_check
    CHECK ((type IN ('EXPENSE', 'INCOME', 'DEBT_PAYMENT') AND amount > 0) OR (type = 'TRANSFER' AND amount <> 0));

ALTER TABLE transactions DROP CONSTRAINT transactions_source_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_source_check
    CHECK (source IN ('BOT', 'MINI_APP', 'RECURRING'));

-- budgets.fixed_payments / loan_payments: NULL now means "auto from recurring rules",
-- a value means the user's manual override wins.
ALTER TABLE budgets ALTER COLUMN fixed_payments DROP NOT NULL;
ALTER TABLE budgets ALTER COLUMN fixed_payments DROP DEFAULT;
ALTER TABLE budgets ALTER COLUMN loan_payments DROP NOT NULL;
ALTER TABLE budgets ALTER COLUMN loan_payments DROP DEFAULT;
