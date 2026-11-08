package uz.zarflow.recurring;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import uz.zarflow.recurring.RecurringDateCalculator.Schedule;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringDateCalculatorTest {

    @Test
    void monthlyDay31FallsBackToLastDayOfShorterMonth() {
        Schedule schedule = Schedule.monthly(31);

        LocalDate april = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2026, 4, 1));

        assertThat(april).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void monthlyDay31InFebruary2028LeapYearLandsOnThe29th() {
        Schedule schedule = Schedule.monthly(31);

        LocalDate feb = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2028, 2, 1));

        assertThat(feb).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void monthlyDay31InFebruary2027NonLeapYearLandsOnThe28th() {
        Schedule schedule = Schedule.monthly(31);

        LocalDate feb = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2027, 2, 1));

        assertThat(feb).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void monthlyAdvancingPastAShortMonthReturnsToTheTrueDayOfMonthNotTheClampedOne() {
        // Jan 31 -> Feb 28 (clamped, 2027 is not leap) -> must roll to Mar 31, not stay near 28.
        Schedule schedule = Schedule.monthly(31);
        LocalDate jan31 = LocalDate.of(2027, 1, 31);

        LocalDate feb = RecurringDateCalculator.nextAfter(schedule, jan31);
        LocalDate mar = RecurringDateCalculator.nextAfter(schedule, feb);

        assertThat(feb).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(mar).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    void monthlyOnOrAfterStaysInTheSameMonthWhenTheDayHasNotPassedYet() {
        Schedule schedule = Schedule.monthly(15);

        LocalDate result = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2026, 11, 1));

        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void monthlyOnOrAfterRollsToNextMonthWhenTheDayAlreadyPassed() {
        Schedule schedule = Schedule.monthly(5);

        LocalDate result = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2026, 11, 10));

        assertThat(result).isEqualTo(LocalDate.of(2026, 12, 5));
    }

    @Test
    void weeklyFindsTheNextMatchingDayOfWeekIncludingToday() {
        Schedule schedule = Schedule.weekly(DayOfWeek.MONDAY);

        LocalDate onMonday = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2026, 11, 9));
        LocalDate onTuesday = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2026, 11, 10));

        assertThat(onMonday).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(onTuesday).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void weeklyAdvancesExactlySevenDays() {
        Schedule schedule = Schedule.weekly(DayOfWeek.MONDAY);

        LocalDate next = RecurringDateCalculator.nextAfter(schedule, LocalDate.of(2026, 11, 9));

        assertThat(next).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void everyNDaysOnOrAfterReturnsTheAnchorWhenAnchorIsNotInThePast() {
        LocalDate anchor = LocalDate.of(2026, 11, 10);
        Schedule schedule = Schedule.everyNDays(10, anchor);

        LocalDate result = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, anchor);

        assertThat(result).isEqualTo(anchor);
    }

    @Test
    void everyNDaysOnOrAfterJumpsForwardWholeCyclesFromAStaleAnchor() {
        // Anchor 2026-01-01, every 10 days; "today" is far in the future.
        LocalDate anchor = LocalDate.of(2026, 1, 1);
        Schedule schedule = Schedule.everyNDays(10, anchor);

        LocalDate result = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2026, 11, 8));

        // Days between anchor and today = 311; 311/10 = 31 whole cycles -> anchor + 310 days = 2026-11-07,
        // which is before "today" (11-08), so one more cycle -> 2026-11-17.
        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(result).isAfterOrEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    void everyNDaysAdvancesByExactlyNDays() {
        Schedule schedule = Schedule.everyNDays(10, LocalDate.of(2026, 1, 1));

        LocalDate next = RecurringDateCalculator.nextAfter(schedule, LocalDate.of(2026, 11, 7));

        assertThat(next).isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test
    void startInThePastResolvesToFirstDueDateOnOrAfterTodayNotTheStaleStartDate() {
        // A rule whose schedule start was years ago must never report a due date in the past.
        Schedule schedule = Schedule.monthly(1);

        LocalDate result = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, LocalDate.of(2026, 11, 8));

        assertThat(result).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(result).isAfterOrEqualTo(LocalDate.of(2026, 11, 8));
    }

}
