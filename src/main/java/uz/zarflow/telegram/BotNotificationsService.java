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
        sendSafely(telegramId, sb.toString(), appButtonOnly(locale));
    }

    @Override
    public void sendDailyReportNoActivity(long telegramId, String language) {
        Locale locale = Locale.of(language);
        String text = messageSource.getMessage("bot.daily-report-empty", null, locale);
        sendSafely(telegramId, text, noActivityButtons(locale));
    }

    private InlineKeyboardMarkup appButtonOnly(Locale locale) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.open-app-button", null, locale))
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
            log.error("Failed to send daily report");
        }
    }

}
