package uz.zarflow.notification;

import java.time.LocalTime;
import java.util.UUID;

/**
 * The notification module's public API for per-user settings. Other modules talk
 * to notifications only through here — never through {@code NotificationSettings}/
 * {@code NotificationSettingsRepository}.
 */
public interface Notifications {

    /** Defaults (enabled, 21:00) when the user has never saved settings. */
    Settings getSettings(UUID userId);

    Settings updateSettings(UUID userId, SettingsUpdate update);

    record Settings(
            boolean dailyReportEnabled, LocalTime dailyReportTime, boolean weeklyReport, boolean monthlyReport) {
    }

    record SettingsUpdate(
            boolean dailyReportEnabled, LocalTime dailyReportTime, boolean weeklyReport, boolean monthlyReport) {
    }

}
