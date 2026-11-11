package uz.zarflow.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.zarflow.telegram.AdminAlerter;

@Component
class DailyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);

    private final DailyReportService dailyReportService;
    private final AdminAlerter adminAlerter;

    DailyReportScheduler(DailyReportService dailyReportService, AdminAlerter adminAlerter) {
        this.dailyReportService = dailyReportService;
        this.adminAlerter = adminAlerter;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "dailyReportScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        try {
            dailyReportService.runDueReports();
        } catch (Exception ex) {
            log.error("Daily report job failed", ex);
            adminAlerter.alert("job:dailyReport", ex);
        }
    }

}
