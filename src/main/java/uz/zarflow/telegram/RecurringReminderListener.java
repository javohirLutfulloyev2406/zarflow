package uz.zarflow.telegram;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import uz.zarflow.account.Accounts;
import uz.zarflow.recurring.Recurring;
import uz.zarflow.recurring.RecurringKind;
import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.common.MoneyFormatter;
import uz.zarflow.user.UserProfiles;

/**
 * Sends recurring-payment reminders and auto-pay confirmations. Lives in {@code telegram}
 * (not {@code recurring}) so {@code recurring} never has to depend on {@code telegram} --
 * matching the one-directional dependency already established for budget-limit alerts.
 */
@Component
class RecurringReminderListener {

    private static final Logger log = LoggerFactory.getLogger(RecurringReminderListener.class);

    private final UserProfiles userProfiles;
    private final Accounts accounts;
    private final TelegramGateway gateway;
    private final MessageSource messageSource;
    private final MoneyFormatter moneyFormatter;

    RecurringReminderListener(
            UserProfiles userProfiles, Accounts accounts, TelegramGateway gateway, MessageSource messageSource,
            MoneyFormatter moneyFormatter) {
        this.userProfiles = userProfiles;
        this.accounts = accounts;
        this.gateway = gateway;
        this.messageSource = messageSource;
        this.moneyFormatter = moneyFormatter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onReminderDue(Recurring.ReminderDue event) {
        UserProfiles.UserSummary user = userProfiles.find(event.userId()).orElse(null);
        if (user == null || user.telegramId() == null) {
            return;
        }
        Locale locale = Locale.of(user.language());
        String amountText = moneyFormatter.format(Money.of(event.amount()), locale);

        if (!event.actionNeeded()) {
            String text = messageSource.getMessage("bot.recurring-auto-paid", new Object[]{event.ruleName(), amountText}, locale);
            sendSafely(user.telegramId(), text, null);
            return;
        }

        if (event.kind() == RecurringKind.INCOME) {
            String text = messageSource.getMessage("bot.recurring-income-due", null, locale);
            sendSafely(user.telegramId(), text, incomeButtons(locale, event.occurrenceId()));
            return;
        }

        String accountName = accounts.listForUser(event.userId()).stream()
                .filter(a -> a.id().equals(event.accountId())).findFirst().map(Accounts.AccountView::name).orElse("");
        String dayLabel = messageSource.getMessage(event.dueToday() ? "bot.recurring-today" : "bot.recurring-tomorrow", null, locale);
        String text = messageSource.getMessage(
                "bot.recurring-reminder", new Object[]{dayLabel, event.ruleName(), amountText, accountName}, locale);
        sendSafely(user.telegramId(), text, paymentButtons(locale, event.occurrenceId()));
    }

    private InlineKeyboardMarkup paymentButtons(Locale locale, UUID occurrenceId) {
        InlineKeyboardButton paid = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.recurring-button-paid", null, locale))
                .callbackData("Y" + occurrenceId)
                .build();
        InlineKeyboardButton remindTomorrow = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.recurring-button-remind-tomorrow", null, locale))
                .callbackData("W" + occurrenceId)
                .build();
        InlineKeyboardButton skip = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.recurring-button-skip", null, locale))
                .callbackData("U" + occurrenceId)
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(paid), new InlineKeyboardRow(remindTomorrow, skip)));
    }

    private InlineKeyboardMarkup incomeButtons(Locale locale, UUID occurrenceId) {
        InlineKeyboardButton yes = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.recurring-button-income-yes", null, locale))
                .callbackData("Y" + occurrenceId)
                .build();
        InlineKeyboardButton no = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.recurring-button-income-no", null, locale))
                .callbackData("W" + occurrenceId)
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(yes, no)));
    }

    private void sendSafely(long chatId, String text, InlineKeyboardMarkup markup) {
        try {
            gateway.sendMessage(chatId, text, markup);
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send recurring reminder");
        }
    }

}
