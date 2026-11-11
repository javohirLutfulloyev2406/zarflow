package uz.zarflow.telegram;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import uz.zarflow.habit.AchievementCode;
import uz.zarflow.savings.Savings;
import uz.zarflow.user.UserProfiles;

/**
 * Sends the "goal reached" congratulation, and (the first time ever) the FIRST_GOAL
 * achievement message too. Lives in {@code telegram} (not {@code savings}) so that
 * {@code savings} never has to depend on {@code telegram}.
 */
@Component
class GoalAchievedListener {

    private static final Logger log = LoggerFactory.getLogger(GoalAchievedListener.class);

    private final UserProfiles userProfiles;
    private final TelegramGateway gateway;
    private final MessageSource messageSource;
    private final BotNotifications botNotifications;

    GoalAchievedListener(
            UserProfiles userProfiles, TelegramGateway gateway, MessageSource messageSource, BotNotifications botNotifications) {
        this.userProfiles = userProfiles;
        this.gateway = gateway;
        this.messageSource = messageSource;
        this.botNotifications = botNotifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onGoalAchieved(Savings.GoalAchieved event) {
        UserProfiles.UserSummary user = userProfiles.find(event.userId()).orElse(null);
        if (user == null || user.telegramId() == null) {
            return;
        }
        Locale locale = Locale.of(user.language());
        String text = messageSource.getMessage("bot.goal-achieved", new Object[]{event.goalName()}, locale);
        try {
            gateway.sendMessage(user.telegramId(), text, null);
        } catch (TelegramGatewayException ex) {
            log.error("Failed to send goal-achieved congratulation");
        }
        if (event.firstGoalAchievementUnlocked()) {
            botNotifications.sendAchievementUnlocked(user.telegramId(), user.language(), AchievementCode.FIRST_GOAL);
        }
    }

}
