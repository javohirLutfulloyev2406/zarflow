package uz.zarflow.notification;

import java.time.LocalTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationsService implements Notifications {

    private static final LocalTime DEFAULT_TIME = LocalTime.of(21, 0);

    private final NotificationSettingsRepository repository;

    NotificationsService(NotificationSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Settings getSettings(UUID userId) {
        return repository.findById(userId)
                .map(s -> new Settings(s.isDailyReportEnabled(), s.getDailyReportTime()))
                .orElseGet(() -> new Settings(true, DEFAULT_TIME));
    }

    @Override
    @Transactional
    public Settings updateSettings(UUID userId, SettingsUpdate update) {
        NotificationSettings settings = repository.findById(userId).orElseGet(() -> new NotificationSettings(userId));
        settings.update(update.dailyReportEnabled(), update.dailyReportTime());
        repository.save(settings);
        return new Settings(settings.isDailyReportEnabled(), settings.getDailyReportTime());
    }

}
