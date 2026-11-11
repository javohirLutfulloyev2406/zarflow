package uz.zarflow.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.zarflow.telegram.AdminAlerter;

/** Also covers month-end leftover evaluation (PRD M9) -- {@link MonthlyReportService#runDueReports()}
 * calls into the savings module in the same pass, so a failure there surfaces as this job's alert too. */
@Component
class MonthlyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportScheduler.class);

    private final MonthlyReportService monthlyReportService;
    private final AdminAlerter adminAlerter;

    MonthlyReportScheduler(MonthlyReportService monthlyReportService, AdminAlerter adminAlerter) {
        this.monthlyReportService = monthlyReportService;
        this.adminAlerter = adminAlerter;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "monthlyReportScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        try {
            monthlyReportService.runDueReports();
        } catch (Exception ex) {
            log.error("Monthly report job failed", ex);
            adminAlerter.alert("job:monthlyReport", ex);
        }
    }

}
