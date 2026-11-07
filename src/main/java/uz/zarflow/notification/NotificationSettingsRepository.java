package uz.zarflow.notification;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

interface NotificationSettingsRepository extends Repository<NotificationSettings, UUID> {

    NotificationSettings save(NotificationSettings settings);

    Optional<NotificationSettings> findById(UUID userId);

}
