package uz.zarflow.recurring;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Pure due-date math for recurring rules (docs/PRD.md M7) -- no Spring, no I/O. Every
 * date here is already in the user's own local timezone; callers resolve that zone
 * before calling in (see {@code uz.zarflow.user.UserProfiles#zoneOf}).
 */
public final class RecurringDateCalculator {

    private RecurringDateCalculator() {
    }

    /**
     * The first occurrence on or after {@code today} -- used both to seed a brand-new
     * rule's {@code next_due_date} (a {@code start_date} in the past must not leave a
     * stale past due date) and, after a pay/skip, to recompute from the day after the
     * just-resolved occurrence.
     */
    public static LocalDate firstOccurrenceOnOrAfter(Schedule schedule, LocalDate today) {
        return switch (schedule.kind()) {
            case MONTHLY -> monthlyOnOrAfter(schedule.dayOfMonth(), today);
            case WEEKLY -> today.with(TemporalAdjusters.nextOrSame(schedule.dayOfWeek()));
            case EVERY_N_DAYS -> everyNDaysOnOrAfter(schedule.anchor(), schedule.everyNDays(), today);
        };
    }

    /** The occurrence strictly after {@code currentDue} -- always re-derived from the rule's
     * own day-of-month/day-of-week/every-N-days, so a clamped short-month date (e.g. Feb 28
     * standing in for day-of-month 31) never drifts the series away from its true target day. */
    public static LocalDate nextAfter(Schedule schedule, LocalDate currentDue) {
        return switch (schedule.kind()) {
            case MONTHLY -> monthlyOnOrAfter(schedule.dayOfMonth(), currentDue.plusDays(1));
            case WEEKLY -> currentDue.plusWeeks(1);
            case EVERY_N_DAYS -> currentDue.plusDays(schedule.everyNDays());
        };
    }

    private static LocalDate monthlyOnOrAfter(int dayOfMonth, LocalDate today) {
        LocalDate thisMonth = clampToMonth(YearMonth.from(today), dayOfMonth);
        if (!thisMonth.isBefore(today)) {
            return thisMonth;
        }
        return clampToMonth(YearMonth.from(today).plusMonths(1), dayOfMonth);
    }

    private static LocalDate clampToMonth(YearMonth month, int dayOfMonth) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }

    private static LocalDate everyNDaysOnOrAfter(LocalDate anchor, int everyNDays, LocalDate today) {
        if (!today.isAfter(anchor)) {
            return anchor;
        }
        long daysSinceAnchor = ChronoUnit.DAYS.between(anchor, today);
        long cyclesElapsed = daysSinceAnchor / everyNDays;
        LocalDate candidate = anchor.plusDays(cyclesElapsed * everyNDays);
        return candidate.isBefore(today) ? candidate.plusDays(everyNDays) : candidate;
    }

    public enum ScheduleKind {
        MONTHLY, WEEKLY, EVERY_N_DAYS
    }

    /** {@code anchor} (the rule's start_date) is only read for EVERY_N_DAYS; MONTHLY/WEEKLY
     * are fully self-contained from dayOfMonth/dayOfWeek. */
    public record Schedule(ScheduleKind kind, Integer dayOfMonth, DayOfWeek dayOfWeek, Integer everyNDays, LocalDate anchor) {

        public static Schedule monthly(int dayOfMonth) {
            return new Schedule(ScheduleKind.MONTHLY, dayOfMonth, null, null, null);
        }

        public static Schedule weekly(DayOfWeek dayOfWeek) {
            return new Schedule(ScheduleKind.WEEKLY, null, dayOfWeek, null, null);
        }

        public static Schedule everyNDays(int everyNDays, LocalDate anchor) {
            return new Schedule(ScheduleKind.EVERY_N_DAYS, null, null, everyNDays, anchor);
        }
    }

}
