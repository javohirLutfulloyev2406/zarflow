package uz.zarflow.savings;

import java.time.YearMonth;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.shared.security.CurrentUser;

/** Local-only: runs the month-end leftover evaluation for the current user on demand,
 * ignoring the "1st of the month, 10:00" gate -- for testing without waiting on real time. */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("local")
class DevSavingsController {

    private final Savings savings;
    private final CurrentUser currentUser;

    DevSavingsController(Savings savings, CurrentUser currentUser) {
        this.savings = savings;
        this.currentUser = currentUser;
    }

    @PostMapping("/leftover/evaluate")
    void evaluateLeftover(@RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        savings.evaluateLeftover(currentUser.requireUserId(), month);
    }

}
