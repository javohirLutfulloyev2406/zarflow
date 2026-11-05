CREATE TABLE categories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users (id) ON DELETE RESTRICT,
    code         VARCHAR(40),
    name_uz      VARCHAR(60) NOT NULL,
    name_ru      VARCHAR(60) NOT NULL,
    type         VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    necessity    VARCHAR(15) CHECK (necessity IN ('ESSENTIAL', 'DISCRETIONARY')),
    icon         VARCHAR(40) NOT NULL,
    color        VARCHAR(7) NOT NULL,
    keywords     TEXT[] NOT NULL DEFAULT '{}',
    sort_order   INT NOT NULL DEFAULT 0,
    archived_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((type = 'EXPENSE' AND necessity IS NOT NULL) OR (type = 'INCOME' AND necessity IS NULL)),
    CHECK (user_id IS NOT NULL OR code IS NOT NULL)
);

CREATE UNIQUE INDEX ux_categories_code_system ON categories (code) WHERE user_id IS NULL;

CREATE INDEX ix_categories_user_id ON categories (user_id);

CREATE INDEX ix_categories_keywords_gin ON categories USING GIN (keywords);
