package uz.zarflow.notification;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.zarflow.shared.common.AdminAlertRequested;

@Component
class DailyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);

    private final DailyReportService dailyReportService;
    private final ApplicationEventPublisher eventPublisher;

    DailyReportScheduler(DailyReportService dailyReportService, ApplicationEventPublisher eventPublisher) {
        this.dailyReportService = dailyReportService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "dailyReportScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        try {
            dailyReportService.runDueReports();
        } catch (Exception ex) {
            log.error("Daily report job failed", ex);
            eventPublisher.publishEvent(new AdminAlertRequested("job:dailyReport", ex));
        }
    }

}
