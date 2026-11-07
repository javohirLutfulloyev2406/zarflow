package uz.zarflow.notification;

import java.time.LocalTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/notifications/settings")
class NotificationSettingsController {

    private final Notifications notifications;
    private final CurrentUser currentUser;

    NotificationSettingsController(Notifications notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    SettingsResponse get() {
        return toResponse(notifications.getSettings(currentUser.requireUserId()));
    }

    @PutMapping
    SettingsResponse update(@Valid @RequestBody UpdateSettingsRequest request) {
        Notifications.SettingsUpdate update = new Notifications.SettingsUpdate(
                request.dailyReportEnabled(), LocalTime.parse(request.dailyReportTime()));
        return toResponse(notifications.updateSettings(currentUser.requireUserId(), update));
    }

    private SettingsResponse toResponse(Notifications.Settings settings) {
        return new SettingsResponse(settings.dailyReportEnabled(), settings.dailyReportTime().toString());
    }

    record SettingsResponse(boolean dailyReportEnabled, String dailyReportTime) {
    }

    record UpdateSettingsRequest(
            @NotNull Boolean dailyReportEnabled,
            @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String dailyReportTime) {
    }

}
