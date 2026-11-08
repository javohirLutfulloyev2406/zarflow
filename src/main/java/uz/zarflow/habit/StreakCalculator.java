package uz.zarflow.habit;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Streak/achievement math over a day-by-day record the caller has already assembled
 * (which local dates "count", and which were within that day's safe-to-spend limit).
 * Pure and Spring-free so it's unit-testable without a context.
 */
public final class StreakCalculator {

    private static final int LIMIT_WEEK_LENGTH = 7;

    private StreakCalculator() {
    }

    public static Result calculate(Input input) {
        List<DayStatus> days = input.days();
        LocalDate today = input.today();

        int currentStreak = currentStreak(days, today);
        int bestStreak = bestStreak(days);
        int daysWithinLimitThisMonth = daysWithinLimitThisMonth(days, input.currentMonth());
        boolean limitWeekAchieved = hasConsecutiveWithinLimitRun(days, LIMIT_WEEK_LENGTH);
        boolean fullMonthAchieved = isMonthFullyCounted(days, input.fullMonthToCheck());

        return new Result(
                currentStreak, bestStreak, daysWithinLimitThisMonth,
                bestStreak >= 7, bestStreak >= 30, limitWeekAchieved, fullMonthAchieved);
    }

    /** Today still "alive" until it has its own record: if today isn't counted yet, the
     * streak is judged as of yesterday instead of being broken by an not-yet-over day. */
    private static int currentStreak(List<DayStatus> days, LocalDate today) {
        int idx = indexOf(days, today);
        if (idx < 0) {
            return 0;
        }
        if (!days.get(idx).counted()) {
            idx--;
        }
        int streak = 0;
        while (idx >= 0 && days.get(idx).counted()) {
            streak++;
            idx--;
        }
        return streak;
    }

    private static int bestStreak(List<DayStatus> days) {
        int best = 0;
        int running = 0;
        for (DayStatus day : days) {
            if (day.counted()) {
                running++;
                best = Math.max(best, running);
            } else {
                running = 0;
            }
        }
        return best;
    }

    private static int daysWithinLimitThisMonth(List<DayStatus> days, YearMonth month) {
        int count = 0;
        for (DayStatus day : days) {
            if (YearMonth.from(day.date()).equals(month) && day.withinLimit()) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasConsecutiveWithinLimitRun(List<DayStatus> days, int length) {
        int running = 0;
        for (DayStatus day : days) {
            running = day.counted() && day.withinLimit() ? running + 1 : 0;
            if (running >= length) {
                return true;
            }
        }
        return false;
    }

    /** {@code null} month (e.g. the current, still-in-progress month) never qualifies. */
    private static boolean isMonthFullyCounted(List<DayStatus> days, YearMonth month) {
        if (month == null) {
            return false;
        }
        LocalDate date = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        while (!date.isAfter(end)) {
            int idx = indexOf(days, date);
            if (idx < 0 || !days.get(idx).counted()) {
                return false;
            }
            date = date.plusDays(1);
        }
        return true;
    }

    /** {@code days} is contiguous and ascending, so this is a direct offset from the first date. */
    private static int indexOf(List<DayStatus> days, LocalDate date) {
        if (days.isEmpty()) {
            return -1;
        }
        long offset = date.toEpochDay() - days.get(0).date().toEpochDay();
        int index = (int) offset;
        if (index < 0 || index >= days.size() || !days.get(index).date().isEqual(date)) {
            return -1;
        }
        return index;
    }

    /** A day "counts" if it has a transaction or a no-spend mark. {@code withinLimit} is only
     * meaningful for counted days with a budget to measure against. */
    public record DayStatus(LocalDate date, boolean counted, boolean withinLimit) {
    }

    /**
     * @param days contiguous, ascending local dates covering the whole lookback window,
     *             ending at {@code today} -- no gaps, even for days that don't count.
     * @param fullMonthToCheck the one month FULL_MONTH should be evaluated against this run
     *                         (normally the just-completed previous month), or {@code null}
     *                         to skip the check entirely.
     */
    public record Input(List<DayStatus> days, LocalDate today, YearMonth currentMonth, YearMonth fullMonthToCheck) {
    }

    public record Result(
            int currentStreak,
            int bestStreak,
            int daysWithinLimitThisMonth,
            boolean streak7Achieved,
            boolean streak30Achieved,
            boolean limitWeekAchieved,
            boolean fullMonthAchieved) {
    }

}
