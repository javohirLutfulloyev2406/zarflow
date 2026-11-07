CREATE TABLE notification_settings (
    user_id               UUID PRIMARY KEY REFERENCES users (id) ON DELETE RESTRICT,
    daily_report_enabled  BOOLEAN NOT NULL DEFAULT true,
    daily_report_time     TIME NOT NULL DEFAULT '21:00',
    last_daily_report_on  DATE
);
