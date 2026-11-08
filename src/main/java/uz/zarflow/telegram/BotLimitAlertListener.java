package uz.zarflow.telegram;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import uz.zarflow.budget.BudgetCalculator;
import uz.zarflow.budget.Budgets;
import uz.zarflow.category.Categories;
import uz.zarflow.user.UserProfiles;

/**
 * Sends the 80%/100% limit-crossing alert. Lives in {@code telegram} (not
 * {@code budget}) so that {@code budget} never has to depend on {@code telegram} --
 * only the reverse, matching every other cross-module dependency this bot already has.
 */
@Component
class BotLimitAlertListener {

    private static final Logger log = LoggerFactory.getLogger(BotLimitAlertListener.class);

    private final UserProfiles userProfiles;
    private final Categories categories;
    private final TelegramGateway gateway;
    private final MessageSource messageSource;
    private final Clock clock;

    BotLimitAlertListener(
            UserProfiles userProfiles, Categories categories, TelegramGateway gateway, MessageSource messageSource, Clock clock) {
        this.userProfiles = userProfiles;
        this.categories = categories;
        this.gateway = gateway;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onLimitThresholdCrossed(Budgets.LimitThresholdCrossed event) {
        UserProfiles.UserSummary user = userProfiles.find(event.userId()).orElse(null);
        if (user == null || user.telegramId() == null) {
            return;
        }
        Locale locale = Locale.of(user.language());
        String categoryLabel = categoryLabel(event.userId(), event.categoryId(), locale);
        ZoneId zone = user.timezone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        long daysLeft = BudgetCalculator.daysLeftInMonth(today, YearMonth.from(today));

        String text = messageSource.getMessage(
                "bot.limit-alert", new Object[]{categoryLabel, event.thresholdPercent(), daysLeft}, locale);
        try {
            gateway.sendMessage(user.telegramId(), text, null);
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send limit alert");
        }
    }

    private String categoryLabel(UUID userId, UUID categoryId, Locale locale) {
        List<Categories.CategoryView> views = categories.listForUser(userId);
        boolean russian = "ru".equals(locale.getLanguage());
        return views.stream()
                .filter(c -> c.id().equals(categoryId)).findFirst()
                .map(c -> russian ? c.nameRu() : c.nameUz())
                .orElse("");
    }

}
