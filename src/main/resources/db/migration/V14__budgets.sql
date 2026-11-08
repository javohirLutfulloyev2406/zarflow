CREATE TABLE budgets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    month             DATE NOT NULL,
    expected_income   NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (expected_income >= 0),
    fixed_payments    NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (fixed_payments >= 0),
    loan_payments     NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (loan_payments >= 0),
    planned_savings   NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (planned_savings >= 0),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- The budget always represents a whole calendar month -- keeping it the 1st of
    -- the month (rather than any day) is what makes (user_id, month) a stable,
    -- unique key callers can look up by YearMonth without a range query.
    CHECK (month = date_trunc('month', month))
);

CREATE UNIQUE INDEX ux_budgets_user_id_month ON budgets (user_id, month);

CREATE TABLE budget_limits (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id     UUID NOT NULL REFERENCES budgets (id) ON DELETE RESTRICT,
    category_id   UUID NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    amount        NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    notified_80   BOOLEAN NOT NULL DEFAULT false,
    notified_100  BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX ux_budget_limits_budget_id_category_id ON budget_limits (budget_id, category_id);

ALTER TABLE transactions ADD COLUMN is_fixed BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE categories ADD COLUMN fixed_default BOOLEAN NOT NULL DEFAULT false;

UPDATE categories SET fixed_default = true WHERE code IN ('home', 'telecom', 'subs');
