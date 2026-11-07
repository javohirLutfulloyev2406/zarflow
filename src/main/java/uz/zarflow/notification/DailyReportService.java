package uz.zarflow.notification;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.category.Categories;
import uz.zarflow.shared.exception.NotFoundException;
import uz.zarflow.telegram.BotNotifications;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@Service
class DailyReportService {

    private final UserProfiles userProfiles;
    private final NotificationSettingsRepository settingsRepository;
    private final Transactions transactions;
    private final Categories categories;
    private final BotNotifications botNotifications;
    private final Clock clock;

    DailyReportService(
            UserProfiles userProfiles,
            NotificationSettingsRepository settingsRepository,
            Transactions transactions,
            Categories categories,
            BotNotifications botNotifications,
            Clock clock) {
        this.userProfiles = userProfiles;
        this.settingsRepository = settingsRepository;
        this.transactions = transactions;
        this.categories = categories;
        this.botNotifications = botNotifications;
        this.clock = clock;
    }

    /** Called every minute by {@link DailyReportScheduler}, guarded by ShedLock against overlap. */
    @Transactional
    void runDueReports() {
        for (UserProfiles.UserSummary user : userProfiles.findAllActive()) {
            if (user.telegramId() == null) {
                continue;
            }
            processUser(user);
        }
    }

    private void processUser(UserProfiles.UserSummary user) {
        ZoneId zone = user.timezone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalTime nowLocal = LocalTime.now(clock.withZone(zone));

        NotificationSettings settings = settingsRepository.findById(user.id()).orElse(null);
        boolean enabled = settings == null || settings.isDailyReportEnabled();
        LocalTime reportTime = settings == null ? LocalTime.of(21, 0) : settings.getDailyReportTime();
        LocalDate lastSent = settings == null ? null : settings.getLastDailyReportOn();

        if (!enabled || nowLocal.isBefore(reportTime) || (lastSent != null && !lastSent.isBefore(today))) {
            return;
        }

        sendReport(user, zone, today);

        NotificationSettings toSave = settings == null ? new NotificationSettings(user.id()) : settings;
        toSave.markSent(today);
        settingsRepository.save(toSave);
    }

    /** For the local-only "send my report now" dev endpoint — ignores the time-of-day/already-sent gate. */
    @Transactional(readOnly = true)
    void sendNow(UUID userId) {
        UserProfiles.UserSummary user = userProfiles.find(userId).orElseThrow(() -> new NotFoundException("user.not_found"));
        ZoneId zone = user.timezone();
        sendReport(user, zone, LocalDate.now(clock.withZone(zone)));
    }

    private void sendReport(UserProfiles.UserSummary user, ZoneId zone, LocalDate date) {
        Transactions.DailyDigest digest = transactions.dailyDigest(user.id(), zone, date);
        if (digest.txCount() == 0) {
            botNotifications.sendDailyReportNoActivity(user.telegramId(), user.language());
            return;
        }
        String topLabel = topCategoryLabel(user.id(), digest.topCategoryId(), Locale.of(user.language()));
        botNotifications.sendDailyReportWithActivity(user.telegramId(), user.language(),
                new BotNotifications.DailyReportActivity(date, digest.income(), digest.expense(), topLabel, digest.topCategoryAmount()));
    }

    private String topCategoryLabel(UUID userId, UUID categoryId, Locale locale) {
        if (categoryId == null) {
            return null;
        }
        List<Categories.CategoryView> views = categories.listForUser(userId);
        boolean russian = "ru".equals(locale.getLanguage());
        return views.stream()
                .filter(c -> c.id().equals(categoryId)).findFirst()
                .map(c -> russian ? c.nameRu() : c.nameUz())
                .orElse(null);
    }

}
