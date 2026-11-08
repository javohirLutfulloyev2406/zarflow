package uz.zarflow.notification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
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

    private boolean weeklyReport;

    private boolean monthlyReport;

    private LocalDate lastWeeklyReportOn;

    /** First day of the MONTH THE REPORT COVERS (not the send date) -- same idea as the
     * other two "last sent" fields, just compared as a YearMonth instead of a LocalDate. */
    private LocalDate lastMonthlyReportOn;

    protected NotificationSettings() {
    }

    NotificationSettings(UUID userId) {
        this.userId = userId;
        this.dailyReportEnabled = true;
        this.dailyReportTime = LocalTime.of(21, 0);
        this.weeklyReport = true;
        this.monthlyReport = true;
    }

    void update(boolean dailyReportEnabled, LocalTime dailyReportTime) {
        this.dailyReportEnabled = dailyReportEnabled;
        this.dailyReportTime = dailyReportTime;
    }

    void updateReportToggles(boolean weeklyReport, boolean monthlyReport) {
        this.weeklyReport = weeklyReport;
        this.monthlyReport = monthlyReport;
    }

    void markSent(LocalDate date) {
        this.lastDailyReportOn = date;
    }

    void markWeeklySent(LocalDate date) {
        this.lastWeeklyReportOn = date;
    }

    void markMonthlySent(YearMonth month) {
        this.lastMonthlyReportOn = month.atDay(1);
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

    boolean isWeeklyReport() {
        return weeklyReport;
    }

    boolean isMonthlyReport() {
        return monthlyReport;
    }

    LocalDate getLastWeeklyReportOn() {
        return lastWeeklyReportOn;
    }

    YearMonth getLastMonthlyReportOn() {
        return lastMonthlyReportOn == null ? null : YearMonth.from(lastMonthlyReportOn);
    }

}
