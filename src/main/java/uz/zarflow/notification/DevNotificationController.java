package uz.zarflow.notification;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.insight.Insights;
import uz.zarflow.shared.exception.BusinessRuleException;
import uz.zarflow.shared.security.CurrentUser;
import uz.zarflow.user.UserProfiles;

/** Local-only: lets the owner trigger their own reports/insight evaluation immediately
 * instead of waiting for the scheduler, and rehearse an arbitrary date. */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("local")
class DevNotificationController {

    private final DailyReportService dailyReportService;
    private final WeeklyReportService weeklyReportService;
    private final MonthlyReportService monthlyReportService;
    private final Insights insights;
    private final UserProfiles userProfiles;
    private final CurrentUser currentUser;
    private final Clock clock;

    DevNotificationController(
            DailyReportService dailyReportService,
            WeeklyReportService weeklyReportService,
            MonthlyReportService monthlyReportService,
            Insights insights,
            UserProfiles userProfiles,
            CurrentUser currentUser,
            Clock clock) {
        this.dailyReportService = dailyReportService;
        this.weeklyReportService = weeklyReportService;
        this.monthlyReportService = monthlyReportService;
        this.insights = insights;
        this.userProfiles = userProfiles;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @PostMapping("/reports/{type}")
    void sendReport(
            @PathVariable String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var userId = currentUser.requireUserId();
        switch (type) {
            case "daily" -> dailyReportService.sendNow(userId, date);
            case "weekly" -> weeklyReportService.sendNow(userId, date);
            case "monthly" -> monthlyReportService.sendNow(userId, date);
            default -> throw new BusinessRuleException("error.validation");
        }
    }

    @PostMapping("/insights/evaluate")
    void evaluateInsights(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var userId = currentUser.requireUserId();
        LocalDate asOf = date != null ? date : LocalDate.now(clock.withZone(userProfiles.zoneOf(userId)));
        insights.evaluateAndStore(userId, asOf);
    }

}
