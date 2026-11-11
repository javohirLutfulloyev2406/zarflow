CREATE TABLE savings_goals (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name           VARCHAR(60) NOT NULL,
    target_amount  NUMERIC(19, 2) NOT NULL CHECK (target_amount > 0),
    target_date    DATE,
    -- Enforced at the application layer (must be an AccountType.SAVINGS account), not here --
    -- the account module owns account-type validation, not this migration.
    account_id     UUID NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    is_emergency   BOOLEAN NOT NULL DEFAULT false,
    status         VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ACHIEVED', 'ARCHIVED')),
    achieved_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_savings_goals_user_id ON savings_goals (user_id) WHERE status = 'ACTIVE';
-- At most one emergency-fund goal per user, and only while it's still active (an archived
-- one shouldn't block creating a fresh emergency goal).
CREATE UNIQUE INDEX ux_savings_goals_one_emergency_per_user ON savings_goals (user_id) WHERE is_emergency AND status = 'ACTIVE';

ALTER TABLE transactions ADD COLUMN linked_goal_id UUID REFERENCES savings_goals (id) ON DELETE SET NULL;

CREATE TABLE leftover_offers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    month         DATE NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    status        VARCHAR(10) NOT NULL DEFAULT 'OFFERED'
                      CHECK (status IN ('OFFERED', 'TO_GOAL', 'TO_DEBT', 'SPLIT', 'DISMISSED')),
    goal_amount   NUMERIC(19, 2),
    debt_amount   NUMERIC(19, 2),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at   TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_leftover_offers_user_id_month ON leftover_offers (user_id, month);
