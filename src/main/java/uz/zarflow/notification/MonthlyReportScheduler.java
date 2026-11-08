package uz.zarflow.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class MonthlyReportScheduler {

    private final MonthlyReportService monthlyReportService;

    MonthlyReportScheduler(MonthlyReportService monthlyReportService) {
        this.monthlyReportService = monthlyReportService;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "monthlyReportScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        monthlyReportService.runDueReports();
    }

}
