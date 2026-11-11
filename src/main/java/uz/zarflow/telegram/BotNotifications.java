package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import uz.zarflow.habit.AchievementCode;

/**
 * The telegram module's public API for proactive (not reply-triggered) messages --
 * daily/weekly/monthly reports and achievement unlocks. Other modules reach Telegram
 * only through here, never through {@code TelegramGateway} directly.
 */
public interface BotNotifications {

    void sendDailyReportWithActivity(long telegramId, String language, DailyReportActivity activity);

    /** {@code insightLine}/{@code streakDays} are null/0 when there's nothing to show. */
    void sendDailyReportNoActivity(long telegramId, String language, String insightLine, int streakDays);

    void sendWeeklyReport(long telegramId, String language, WeeklyReportActivity activity);

    void sendMonthlyReport(long telegramId, String language, MonthlyReportActivity activity);

    void sendAchievementUnlocked(long telegramId, String language, AchievementCode code);

    /** {@code insightLine}/{@code streakDays} are null/0 when there's nothing to show. */
    record DailyReportActivity(
            LocalDate date, BigDecimal income, BigDecimal expense, String topCategoryLabel, BigDecimal topCategoryAmount,
            String insightLine, int streakDays) {
    }

    record CategoryShare(String name, BigDecimal amount) {
    }

    record WeeklyReportActivity(
            LocalDate from, LocalDate to, BigDecimal total, BigDecimal dailyAverage,
            LocalDate highestDay, BigDecimal highestDayAmount, List<CategoryShare> topCategories,
            BigDecimal changeVsLastWeekPercent, int streakDays) {
    }

    record MonthlyReportActivity(
            YearMonth month, BigDecimal income, BigDecimal expense, BigDecimal net, BigDecimal debtPayments,
            BigDecimal savings, List<CategoryShare> topCategories, BigDecimal changeVsPrevMonthPercent,
            BigDecimal goalSavings, BigDecimal goalSavingsPercent) {
    }

}
