CREATE TABLE day_marks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    date        DATE NOT NULL,
    no_spend    BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_day_marks_user_id_date ON day_marks (user_id, date);

CREATE TABLE insights (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    code            VARCHAR(30) NOT NULL,
    period_key      VARCHAR(10) NOT NULL,
    payload         JSONB NOT NULL,
    priority        INTEGER NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_in_bot_at  TIMESTAMPTZ,
    dismissed_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_insights_user_code_period ON insights (user_id, code, period_key);
-- The "top unsent insight for today's report" / "active insights list" query shape.
CREATE INDEX ix_insights_active_by_priority ON insights (user_id, priority) WHERE dismissed_at IS NULL;

CREATE TABLE user_achievements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    code         VARCHAR(20) NOT NULL,
    achieved_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_user_achievements_user_id_code ON user_achievements (user_id, code);

ALTER TABLE notification_settings ADD COLUMN weekly_report BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE notification_settings ADD COLUMN monthly_report BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE notification_settings ADD COLUMN last_weekly_report_on DATE;
-- Stores the first day of the MONTH THE REPORT COVERS (not the send date) so the gate is a
-- plain YearMonth comparison, same idea as last_daily_report_on/last_weekly_report_on.
ALTER TABLE notification_settings ADD COLUMN last_monthly_report_on DATE;
