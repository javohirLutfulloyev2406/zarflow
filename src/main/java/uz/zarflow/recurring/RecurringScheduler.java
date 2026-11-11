package uz.zarflow.recurring;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.zarflow.telegram.AdminAlerter;

@Component
class RecurringScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringScheduler.class);

    private final RecurringReminderSweepService sweepService;
    private final AdminAlerter adminAlerter;

    RecurringScheduler(RecurringReminderSweepService sweepService, AdminAlerter adminAlerter) {
        this.sweepService = sweepService;
        this.adminAlerter = adminAlerter;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "recurringReminderScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        try {
            sweepService.runDueSweep();
        } catch (Exception ex) {
            log.error("Recurring reminder job failed", ex);
            adminAlerter.alert("job:recurring", ex);
        }
    }

}
