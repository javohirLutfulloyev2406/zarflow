CREATE TABLE debts (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    name                   VARCHAR(60) NOT NULL,
    lender                 VARCHAR(60),
    type                   VARCHAR(20) NOT NULL
                               CHECK (type IN ('BANK_LOAN', 'INSTALLMENT', 'CREDIT_CARD', 'PERSONAL_DEBT', 'OTHER')),
    original_amount        NUMERIC(19, 2) NOT NULL CHECK (original_amount > 0),
    outstanding_principal  NUMERIC(19, 2) NOT NULL CHECK (outstanding_principal >= 0),
    annual_rate            NUMERIC(7, 4) NOT NULL CHECK (annual_rate >= 0),
    monthly_payment        NUMERIC(19, 2) NOT NULL CHECK (monthly_payment > 0),
    start_date             DATE NOT NULL,
    end_date               DATE NOT NULL,
    payment_day            INTEGER NOT NULL CHECK (payment_day BETWEEN 1 AND 31),
    interest_method        VARCHAR(20) NOT NULL CHECK (interest_method IN ('ANNUITY_MONTHLY', 'ACTUAL_365')),
    status                 VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CLOSED')),
    closed_at              TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_debts_user_id ON debts (user_id) WHERE status = 'ACTIVE';

CREATE TABLE debt_schedule (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id        UUID NOT NULL REFERENCES debts (id) ON DELETE RESTRICT,
    seq            INTEGER NOT NULL,
    due_date       DATE NOT NULL,
    principal      NUMERIC(19, 2) NOT NULL,
    interest       NUMERIC(19, 2) NOT NULL,
    fee            NUMERIC(19, 2) NOT NULL DEFAULT 0,
    is_estimated   BOOLEAN NOT NULL DEFAULT true,
    paid_payment_id UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_debt_schedule_debt_id_seq ON debt_schedule (debt_id, seq);
CREATE INDEX ix_debt_schedule_debt_id_due_date ON debt_schedule (debt_id, due_date);

CREATE TABLE debt_payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id         UUID NOT NULL REFERENCES debts (id) ON DELETE RESTRICT,
    paid_on         DATE NOT NULL,
    principal       NUMERIC(19, 2) NOT NULL CHECK (principal >= 0),
    interest        NUMERIC(19, 2) NOT NULL CHECK (interest >= 0),
    fee             NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (fee >= 0),
    extra           BOOLEAN NOT NULL DEFAULT false,
    transaction_id  UUID NOT NULL REFERENCES transactions (id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_debt_payments_debt_id ON debt_payments (debt_id);
-- The analytics module's raw material for "interest + fee this month, by date" -- scans by
-- paid_on across all of a user's debts, so it needs the debt_id -> user_id join to be cheap.
CREATE INDEX ix_debt_payments_paid_on ON debt_payments (paid_on);

-- A paid schedule row points back at the payment that settled it (nullable: unpaid rows, and
-- rows settled by a payment that didn't match one-to-one, have none).
ALTER TABLE debt_schedule ADD CONSTRAINT debt_schedule_paid_payment_id_fkey
    FOREIGN KEY (paid_payment_id) REFERENCES debt_payments (id) ON DELETE SET NULL;

-- LOAN recurring rules can optionally link to a specific debt so their [To'landi] flow
-- records a tracked debt payment (principal/interest split + outstanding update) instead of
-- just a bare DEBT_PAYMENT transaction.
ALTER TABLE recurring_rules ADD COLUMN debt_id UUID REFERENCES debts (id) ON DELETE SET NULL;
