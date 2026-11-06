ALTER TABLE users
    ADD COLUMN onboarding_completed_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
