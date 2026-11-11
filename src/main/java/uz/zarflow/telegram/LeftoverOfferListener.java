package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import uz.zarflow.debt.DebtStatus;
import uz.zarflow.debt.Debts;
import uz.zarflow.savings.Savings;
import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.common.MoneyFormatter;
import uz.zarflow.user.UserProfiles;

/**
 * Sends the month-end leftover routing offer (PRD S6): free-budget leftover plus a
 * one-line debt-simulator teaser and four routing buttons. Lives in {@code telegram}
 * (not {@code savings}) so {@code savings} never has to depend on {@code telegram}.
 */
@Component
class LeftoverOfferListener {

    private static final Logger log = LoggerFactory.getLogger(LeftoverOfferListener.class);
    private static final int DAYS_PER_MONTH = 30;

    private final UserProfiles userProfiles;
    private final Debts debts;
    private final TelegramGateway gateway;
    private final MessageSource messageSource;
    private final MoneyFormatter moneyFormatter;

    LeftoverOfferListener(
            UserProfiles userProfiles, Debts debts, TelegramGateway gateway, MessageSource messageSource,
            MoneyFormatter moneyFormatter) {
        this.userProfiles = userProfiles;
        this.debts = debts;
        this.gateway = gateway;
        this.messageSource = messageSource;
        this.moneyFormatter = moneyFormatter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onLeftoverOffered(Savings.LeftoverOffered event) {
        UserProfiles.UserSummary user = userProfiles.find(event.userId()).orElse(null);
        if (user == null || user.telegramId() == null) {
            return;
        }
        Locale locale = Locale.of(user.language());
        String amountText = moneyFormatter.format(Money.of(event.amount()), locale);
        StringBuilder sb = new StringBuilder(messageSource.getMessage("bot.leftover-offer", new Object[]{amountText}, locale));

        debtHint(event.userId(), event.amount(), locale).ifPresent(hint -> sb.append('\n').append(hint));

        String payload = event.month().toString();
        try {
            gateway.sendMessage(user.telegramId(), sb.toString(), leftoverButtons(locale, payload));
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send leftover offer");
        }
    }

    private java.util.Optional<String> debtHint(java.util.UUID userId, BigDecimal leftoverAmount, Locale locale) {
        List<Debts.DebtView> active = debts.listForUser(userId).stream()
                .filter(d -> d.status() == DebtStatus.ACTIVE)
                .toList();
        if (active.isEmpty()) {
            return java.util.Optional.empty();
        }
        Debts.SimulateView simulation = debts.simulate(userId, active.get(0).id(), leftoverAmount);
        long daysCloser = (long) simulation.monthsSaved() * DAYS_PER_MONTH;
        if (daysCloser <= 0 && simulation.interestSaved().signum() <= 0) {
            return java.util.Optional.empty();
        }
        String interestText = moneyFormatter.format(Money.of(simulation.interestSaved()), locale);
        return java.util.Optional.of(messageSource.getMessage(
                "bot.leftover-offer-debt-hint", new Object[]{daysCloser, interestText}, locale));
    }

    private InlineKeyboardMarkup leftoverButtons(Locale locale, String payload) {
        InlineKeyboardButton toGoal = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.leftover-button-to-goal", null, locale))
                .callbackData("L" + payload)
                .build();
        InlineKeyboardButton toDebt = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.leftover-button-to-debt", null, locale))
                .callbackData("E" + payload)
                .build();
        InlineKeyboardButton stayInApp = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.leftover-button-stay-in-app", null, locale))
                .callbackData("B" + payload)
                .build();
        InlineKeyboardButton later = InlineKeyboardButton.builder()
                .text(messageSource.getMessage("bot.leftover-button-later", null, locale))
                .callbackData("T" + payload)
                .build();
        return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(toGoal, toDebt), new InlineKeyboardRow(stayInApp, later)));
    }

}
