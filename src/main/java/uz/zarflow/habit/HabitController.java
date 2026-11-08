package uz.zarflow.habit;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/habits")
class HabitController {

    private final Habits habits;
    private final CurrentUser currentUser;

    HabitController(Habits habits, CurrentUser currentUser) {
        this.habits = habits;
        this.currentUser = currentUser;
    }

    @GetMapping
    HabitSummaryResponse get() {
        Habits.HabitSummary summary = habits.getSummary(currentUser.requireUserId());
        List<AchievementResponse> achievements = summary.achievements().stream()
                .map(a -> new AchievementResponse(a.code(), a.unlocked(), a.achievedAt()))
                .toList();
        return new HabitSummaryResponse(
                summary.currentStreak(), summary.bestStreak(), summary.daysWithinLimitThisMonth(), achievements);
    }

    record AchievementResponse(AchievementCode code, boolean unlocked, Instant achievedAt) {
    }

    record HabitSummaryResponse(
            int currentStreak, int bestStreak, int daysWithinLimitThisMonth, List<AchievementResponse> achievements) {
    }

}
