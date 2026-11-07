package uz.zarflow.notification;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.shared.security.CurrentUser;

/** Local-only: lets the owner trigger their own daily report immediately instead of waiting for the scheduler. */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("local")
class DevNotificationController {

    private final DailyReportService dailyReportService;
    private final CurrentUser currentUser;

    DevNotificationController(DailyReportService dailyReportService, CurrentUser currentUser) {
        this.dailyReportService = dailyReportService;
        this.currentUser = currentUser;
    }

    @PostMapping("/send-daily-report")
    void sendDailyReport() {
        dailyReportService.sendNow(currentUser.requireUserId());
    }

}
