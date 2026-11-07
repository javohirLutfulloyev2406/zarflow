package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The telegram module's public API for proactive (not reply-triggered) messages —
 * today just the scheduled daily report. Other modules reach Telegram only through
 * here, never through {@code TelegramGateway} directly.
 */
public interface BotNotifications {

    void sendDailyReportWithActivity(long telegramId, String language, DailyReportActivity activity);

    void sendDailyReportNoActivity(long telegramId, String language);

    record DailyReportActivity(
            LocalDate date, BigDecimal income, BigDecimal expense, String topCategoryLabel, BigDecimal topCategoryAmount) {
    }

}
