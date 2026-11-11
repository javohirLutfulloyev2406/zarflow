package uz.zarflow.recurring;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.zarflow.shared.common.AdminAlertRequested;

@Component
class RecurringScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringScheduler.class);

    private final RecurringReminderSweepService sweepService;
    private final ApplicationEventPublisher eventPublisher;

    RecurringScheduler(RecurringReminderSweepService sweepService, ApplicationEventPublisher eventPublisher) {
        this.sweepService = sweepService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "recurringReminderScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        try {
            sweepService.runDueSweep();
        } catch (Exception ex) {
            log.error("Recurring reminder job failed", ex);
            eventPublisher.publishEvent(new AdminAlertRequested("job:recurring", ex));
        }
    }

}
