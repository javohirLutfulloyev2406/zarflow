package uz.zarflow.recurring;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class RecurringScheduler {

    private final RecurringReminderSweepService sweepService;

    RecurringScheduler(RecurringReminderSweepService sweepService) {
        this.sweepService = sweepService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "recurringReminderScheduler", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    void run() {
        sweepService.runDueSweep();
    }

}
