package uz.zarflow.recurring;

import java.time.LocalDate;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Local-only: runs the recurring reminder/auto-create sweep as if {@code date} were today,
 * across every user, ignoring the 10:00 time gate -- for testing without waiting on real time. */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("local")
class DevRecurringController {

    private final RecurringReminderSweepService sweepService;

    DevRecurringController(RecurringReminderSweepService sweepService) {
        this.sweepService = sweepService;
    }

    @PostMapping("/run-recurring")
    void runRecurring(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        sweepService.runForDate(date);
    }

}
