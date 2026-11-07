package uz.zarflow.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class DailyReportScheduler {

    private final DailyReportService dailyReportService;

    DailyReportScheduler(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "dailyReportScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        dailyReportService.runDueReports();
    }

}
