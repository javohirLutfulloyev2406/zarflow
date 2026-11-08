package uz.zarflow.habit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The habit module's public API: no-spend day marks, streaks, and achievements. Other
 * modules talk to habits only through here — never through the entities/repositories.
 */
public interface Habits {

    /** Idempotent: marking the same date twice is a no-op the second time. */
    void markNoSpend(UUID userId, LocalDate date);

    HabitSummary getSummary(UUID userId);

    /** Local dates in the range that "count" (transaction or no-spend mark) -- the insight
     * module's raw material for the "nothing logged in N days" nudge. */
    Set<LocalDate> countedDatesBetween(UUID userId, LocalDate from, LocalDate toInclusive);

    /** Recomputes streaks against the latest data and persists any achievement that newly
     * qualifies. Returns only the ones unlocked by THIS call (empty most days) -- callers
     * use that to send a one-time congratulation, never a general "you have N achievements". */
    List<AchievementCode> recomputeAndUnlockAchievements(UUID userId);

    record HabitSummary(
            int currentStreak, int bestStreak, int daysWithinLimitThisMonth, List<AchievementView> achievements) {
    }

    record AchievementView(AchievementCode code, boolean unlocked, Instant achievedAt) {
    }

}
