CREATE TABLE users (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    telegram_id              BIGINT NOT NULL UNIQUE,
    first_name               VARCHAR(100),
    last_name                VARCHAR(100),
    username                 VARCHAR(64),
    language                 VARCHAR(5) NOT NULL DEFAULT 'uz' CHECK (language IN ('uz', 'ru')),
    timezone                 VARCHAR(40) NOT NULL DEFAULT 'Asia/Tashkent',
    expected_monthly_income  NUMERIC(19, 2) CHECK (expected_monthly_income >= 0),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ
);
