package uz.zarflow.telegram;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import uz.zarflow.habit.AchievementCode;
import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.common.MoneyFormatter;
import uz.zarflow.shared.common.MonthNames;

@Service
class BotNotificationsService implements BotNotifications {

    private static final Logger log = LoggerFactory.getLogger(BotNotificationsService.class);

    private final TelegramGateway gateway;
    private final MessageSource messageSource;
    private final MoneyFormatter moneyFormatter;
    private final TelegramBotProperties properties;

    BotNotificationsService(
            TelegramGateway gateway, MessageSource messageSource, MoneyFormatter moneyFormatter, TelegramBotProperties properties) {
        this.gateway = gateway;
        this.messageSource = messageSource;
        this.moneyFormatter = moneyFormatter;
        this.properties = properties;
    }

    @Override
    public void sendDailyReportWithActivity(long telegramId, String language, DailyReportActivity activity) {
        Locale locale = Locale.of(language);
        String dateLabel = MonthNames.dayAndMonth(activity.date(), locale);

        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage("bot.daily-report-header", new Object[]{dateLabel}, locale)).append('\n');
        sb.append(messageSource.getMessage(
                "bot.daily-report-income", new Object[]{moneyFormatter.format(Money.of(activity.income()), locale)}, locale))
                .append('\n');
        sb.append(messageSource.getMessage(
                "bot.daily-report-expense", new Object[]{moneyFormatter.format(Money.of(activity.expense()), locale)}, locale));
        if (activity.topCategoryLabel() != null) {
            sb.append('\n').append(messageSource.getMessage("bot.daily-report-top",
                    new Object[]{activity.topCategoryLabel(), moneyFormatter.format(Money.of(activity.topCategoryAmount()), locale)},
                    locale));
        }
        appendInsightAndStreak(sb, locale, activity.insightLine(), activity.streakDays());
        sendSafely(telegramId, sb.toString(), appButtonOnly(locale));
    }

    @Override
    public void sendDailyReportNoActivity(long telegramId, String language, String insightLine, int streakDays) {
        Locale locale = Locale.of(language);
        StringBuilder sb = new StringBuilder(messageSource.getMessage("bot.daily-report-empty", null, locale));
        appendInsightAndStreak(sb, locale, insightLine, streakDays);
        sendSafely(telegramId, sb.toString(), noActivityButtons(locale));
    }

    @Override
    public void sendWeeklyReport(long telegramId, String language, WeeklyReportActivity activity) {
        Locale locale = Locale.of(language);
        if (activity.total().signum() == 0) {
            sendSafely(telegramId, messageSource.getMessage("bot.weekly-report-empty", null, locale), appButtonOnly(locale));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage("bot.weekly-report-header",
                new Object[]{MonthNames.dayAndMonth(activity.from(), locale), MonthNames.dayAndMonth(activity.to(), locale)}, locale))
                .append('\n');
        sb.append(messageSource.getMessage(
                "bot.weekly-report-total", new Object[]{moneyFormatter.format(Money.of(activity.total()), locale)}, locale))
                .append('\n');
        sb.append(messageSource.getMessage("bot.weekly-report-average",
                new Object[]{moneyFormatter.format(Money.of(activity.dailyAverage()), locale)}, locale));
        if (activity.highestDay() != null) {
            sb.append('\n').append(messageSource.getMessage("bot.weekly-report-highest",
                    new Object[]{MonthNames.dayAndMonth(activity.highestDay(), locale),
                            moneyFormatter.format(Money.of(activity.highestDayAmount()), locale)}, locale));
        }
        appendTopCategories(sb, "bot.weekly-report-top-categories", activity.topCategories(), locale);
        if (activity.changeVsLastWeekPercent() != null) {
            sb.append('\n').append(messageSource.getMessage(
                    "bot.weekly-report-change", new Object[]{activity.changeVsLastWeekPercent().toPlainString()}, locale));
        }
        if (activity.streakDays() > 0) {
            sb.append('\n').append(messageSource.getMessage("bot.weekly-report-streak", new Object[]{activity.streakDays()}, locale));
        }
        sendSafely(telegramId, sb.toString(), appButtonOnly(locale));
    }

    @Override
    public void sendMonthlyReport(long telegramId, String language, MonthlyReportActivity activity) {
        Locale locale = Locale.of(language);
        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage(
                "bot.monthly-report-header", new Object[]{MonthNames.monthAndYear(activity.month(), locale)}, locale))
                .append('\n');
        sb.append(messageSource.getMessage(
                "bot.monthly-report-income", new Object[]{moneyFormatter.format(Money.of(activity.income()), locale)}, locale))
                .append('\n');
        sb.append(messageSource.getMessage(
                "bot.monthly-report-expense", new Object[]{moneyFormatter.format(Money.of(activity.expense()), locale)}, locale))
                .append('\n');
        sb.append(messageSource.getMessage(
                "bot.monthly-report-net", new Object[]{moneyFormatter.format(Money.of(activity.net()), locale)}, locale));
        if (activity.debtPayments() != null && activity.debtPayments().signum() > 0) {
            sb.append('\n').append(messageSource.getMessage(
                    "bot.monthly-report-debt", new Object[]{moneyFormatter.format(Money.of(activity.debtPayments()), locale)}, locale));
        }
        if (activity.savings() != null) {
            sb.append('\n').append(messageSource.getMessage(
                    "bot.monthly-report-savings", new Object[]{moneyFormatter.format(Money.of(activity.savings()), locale)}, locale));
        }
        appendTopCategories(sb, "bot.monthly-report-top-categories", activity.topCategories(), locale);
        if (activity.changeVsPrevMonthPercent() != null) {
            sb.append('\n').append(messageSource.getMessage(
                    "bot.monthly-report-change", new Object[]{activity.changeVsPrevMonthPercent().toPlainString()}, locale));
        }
        sendSafely(telegramId, sb.toString(), monthlyReportButtons(locale));
    }

    @Override
    public void sendAchievementUnlocked(long telegramId, String language, AchievementCode code) {
        Locale locale = Locale.of(language);
        String key = switch (code) {
            case STREAK_7 -> "bot.achievement-streak-7";
            case STREAK_30 -> "bot.achievement-streak-30";
            case LIMIT_WEEK -> "bot.achievement-limit-week";
            case FULL_MONTH -> "bot.achievement-full-month";
        };
        sendSafely(telegramId, messageSource.getMessage(key, null, locale), appButtonOnly(locale));
    }

    private void appendInsightAndStreak(StringBuilder sb, Locale locale, String insightLine, int streakDays) {
        if (insightLine != null) {
            sb.append("\n\n").append(insightLine);
        }
        if (streakDays > 0) {
            sb.append('\n').append(messageSource.getMessage("bot.daily-report-streak", new Object[]{streakDays}, locale));
        }
    }

    private void appendTopCategories(StringBuilder sb, String headerKey, List<CategoryShare> topCategories, Locale locale) {
        if (topCategories.isEmpty()) {
            return;
        }
        sb.append('\n').append(messageSource.getMessage(headerKey, null, locale));
        for (CategoryShare category : topCategories) {
            sb.append('\n').append(category.name()).append(" — ")
                    .append(moneyFormatter.format(Money.of(category.amount()), locale));
        }
    }

    private InlineKeyboardMarkup appButtonOnly(Locale locale) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.open-app-button", null, locale))
                .webApp(new WebAppInfo(properties.miniAppUrl()))
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(button)));
    }

    private InlineKeyboardMarkup monthlyReportButtons(Locale locale) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.monthly-report-open-app-button", null, locale))
                .webApp(new WebAppInfo(properties.miniAppUrl()))
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(button)));
    }

    private InlineKeyboardMarkup noActivityButtons(Locale locale) {
        InlineKeyboardButton noExpense = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.daily-report-no-expense-button", null, locale))
                .callbackData("Z")
                .build();
        InlineKeyboardButton openApp = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.open-app-button", null, locale))
                .webApp(new WebAppInfo(properties.miniAppUrl()))
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(noExpense, openApp)));
    }

    private void sendSafely(long chatId, String text, InlineKeyboardMarkup markup) {
        try {
            gateway.sendMessage(chatId, text, markup);
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send bot notification");
        }
    }

}
