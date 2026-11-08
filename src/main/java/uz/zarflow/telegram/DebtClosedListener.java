package uz.zarflow.telegram;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import uz.zarflow.debt.Debts;
import uz.zarflow.user.UserProfiles;

/**
 * Sends the "debt fully paid off" congratulation. Lives in {@code telegram} (not
 * {@code debt}) so that {@code debt} never has to depend on {@code telegram} -- only the
 * reverse, matching every other cross-module dependency this bot already has.
 */
@Component
class DebtClosedListener {

    private static final Logger log = LoggerFactory.getLogger(DebtClosedListener.class);

    private final UserProfiles userProfiles;
    private final TelegramGateway gateway;
    private final MessageSource messageSource;

    DebtClosedListener(UserProfiles userProfiles, TelegramGateway gateway, MessageSource messageSource) {
        this.userProfiles = userProfiles;
        this.gateway = gateway;
        this.messageSource = messageSource;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onDebtClosed(Debts.DebtClosed event) {
        UserProfiles.UserSummary user = userProfiles.find(event.userId()).orElse(null);
        if (user == null || user.telegramId() == null) {
            return;
        }
        Locale locale = Locale.of(user.language());
        String text = messageSource.getMessage("bot.debt-congrats", new Object[]{event.debtName()}, locale);
        try {
            gateway.sendMessage(user.telegramId(), text, null);
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send debt-closed congratulation");
        }
    }

}
