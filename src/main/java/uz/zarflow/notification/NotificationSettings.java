package uz.zarflow.notification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_settings")
class NotificationSettings {

    @Id
    private UUID userId;

    private boolean dailyReportEnabled;

    private LocalTime dailyReportTime;

    private LocalDate lastDailyReportOn;

    protected NotificationSettings() {
    }

    NotificationSettings(UUID userId) {
        this.userId = userId;
        this.dailyReportEnabled = true;
        this.dailyReportTime = LocalTime.of(21, 0);
    }

    void update(boolean dailyReportEnabled, LocalTime dailyReportTime) {
        this.dailyReportEnabled = dailyReportEnabled;
        this.dailyReportTime = dailyReportTime;
    }

    void markSent(LocalDate date) {
        this.lastDailyReportOn = date;
    }

    UUID getUserId() {
        return userId;
    }

    boolean isDailyReportEnabled() {
        return dailyReportEnabled;
    }

    LocalTime getDailyReportTime() {
        return dailyReportTime;
    }

    LocalDate getLastDailyReportOn() {
        return lastDailyReportOn;
    }

}
