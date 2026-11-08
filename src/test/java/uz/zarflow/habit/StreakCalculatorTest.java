package uz.zarflow.habit;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.habit.StreakCalculator.DayStatus;
import uz.zarflow.habit.StreakCalculator.Input;
import uz.zarflow.habit.StreakCalculator.Result;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    @Test
    void currentStreakCountsConsecutiveCountedDaysEndingToday() {
        List<DayStatus> days = contiguousDays(TODAY.minusDays(4), TODAY, date -> true, date -> false);
        Result result = calculate(days, TODAY, null);
        assertThat(result.currentStreak()).isEqualTo(5);
    }

    @Test
    void gapBreaksCurrentStreakButBestStreakRemembersThePriorRun() {
        // counted: Nov1 Nov2 Nov3 [gap Nov4] Nov5 Nov6(=today)
        List<DayStatus> days = new ArrayList<>();
        days.add(day(TODAY.minusDays(5), true));
        days.add(day(TODAY.minusDays(4), true));
        days.add(day(TODAY.minusDays(3), true));
        days.add(day(TODAY.minusDays(2), false));
        days.add(day(TODAY.minusDays(1), true));
        days.add(day(TODAY, true));

        Result result = calculate(days, TODAY, null);
        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.bestStreak()).isEqualTo(3);
    }

    @Test
    void todayNotYetRecordedKeepsYesterdaysStreakAliveInsteadOfResettingToZero() {
        List<DayStatus> days = new ArrayList<>();
        days.add(day(TODAY.minusDays(2), true));
        days.add(day(TODAY.minusDays(1), true));
        days.add(day(TODAY, false)); // today has no transaction/mark yet

        Result result = calculate(days, TODAY, null);
        assertThat(result.currentStreak()).isEqualTo(2);
    }

    @Test
    void todayNotRecordedAfterAYesterdayGapIsZeroNotNegative() {
        List<DayStatus> days = new ArrayList<>();
        days.add(day(TODAY.minusDays(2), true));
        days.add(day(TODAY.minusDays(1), false));
        days.add(day(TODAY, false));

        Result result = calculate(days, TODAY, null);
        assertThat(result.currentStreak()).isEqualTo(0);
    }

    @Test
    void noSpendMarkedDaysCountTheSameAsTransactionDays() {
        // "counted" alone decides the streak -- caller folds transactions OR no_spend into it.
        List<DayStatus> days = new ArrayList<>();
        days.add(day(TODAY.minusDays(1), true));
        days.add(day(TODAY, true));

        Result result = calculate(days, TODAY, null);
        assertThat(result.currentStreak()).isEqualTo(2);
    }

    @Test
    void streak7And30AchievementsFireOffBestStreakSoAPastPeakStillCounts() {
        List<DayStatus> days = contiguousDays(TODAY.minusDays(9), TODAY, date -> !date.isEqual(TODAY), date -> false);
        // 9 counted days in a row (today itself not counted), best streak = 9.
        Result result = calculate(days, TODAY, null);
        assertThat(result.bestStreak()).isEqualTo(9);
        assertThat(result.streak7Achieved()).isTrue();
        assertThat(result.streak30Achieved()).isFalse();
    }

    @Test
    void limitWeekAchievementNeedsSevenConsecutiveCountedAndWithinLimitDays() {
        List<DayStatus> days = contiguousDays(TODAY.minusDays(9), TODAY, date -> true, date -> true);
        Result result = calculate(days, TODAY, null);
        assertThat(result.limitWeekAchieved()).isTrue();
    }

    @Test
    void limitWeekAchievementDoesNotFireWhenAWithinLimitRunIsShorterThanSevenDays() {
        List<DayStatus> days = contiguousDays(TODAY.minusDays(5), TODAY, date -> true, date -> true);
        Result result = calculate(days, TODAY, null);
        assertThat(result.limitWeekAchieved()).isFalse();
    }

    @Test
    void daysWithinLimitThisMonthOnlyCountsDatesInTheGivenMonth() {
        YearMonth november = YearMonth.of(2026, 11);
        List<DayStatus> days = contiguousDays(LocalDate.of(2026, 10, 28), TODAY, date -> true, date -> true);
        Result result = calculate(days, TODAY, november, null);
        // Nov 1..8 inclusive = 8 days within the month.
        assertThat(result.daysWithinLimitThisMonth()).isEqualTo(8);
    }

    @Test
    void fullMonthAchievementRequiresEveryDayOfThatMonthToBeCounted() {
        YearMonth october = YearMonth.of(2026, 10);
        List<DayStatus> days = contiguousDays(LocalDate.of(2026, 10, 1), TODAY, date -> true, date -> false);
        Result result = calculate(days, TODAY, october);
        assertThat(result.fullMonthAchieved()).isTrue();
    }

    @Test
    void fullMonthAchievementFailsWhenEvenOneDayOfThatMonthWasNotCounted() {
        YearMonth october = YearMonth.of(2026, 10);
        List<DayStatus> days = contiguousDays(
                LocalDate.of(2026, 10, 1), TODAY, date -> !date.isEqual(LocalDate.of(2026, 10, 15)), date -> false);
        Result result = calculate(days, TODAY, october);
        assertThat(result.fullMonthAchieved()).isFalse();
    }

    @Test
    void fullMonthAchievementNeverFiresForTheStillInProgressCurrentMonth() {
        List<DayStatus> days = contiguousDays(LocalDate.of(2026, 11, 1), TODAY, date -> true, date -> false);
        Result result = calculate(days, TODAY, null);
        assertThat(result.fullMonthAchieved()).isFalse();
    }

    @Test
    void noDataAtAllMeansZeroStreaksAndNoAchievements() {
        List<DayStatus> days = contiguousDays(TODAY.minusDays(10), TODAY, date -> false, date -> false);
        Result result = calculate(days, TODAY, null);
        assertThat(result.currentStreak()).isZero();
        assertThat(result.bestStreak()).isZero();
        assertThat(result.streak7Achieved()).isFalse();
        assertThat(result.limitWeekAchieved()).isFalse();
    }

    private static DayStatus day(LocalDate date, boolean counted) {
        return new DayStatus(date, counted, false);
    }

    private static List<DayStatus> contiguousDays(
            LocalDate from, LocalDate toInclusive, java.util.function.Predicate<LocalDate> counted,
            java.util.function.Predicate<LocalDate> withinLimit) {
        List<DayStatus> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(toInclusive); d = d.plusDays(1)) {
            days.add(new DayStatus(d, counted.test(d), withinLimit.test(d)));
        }
        return days;
    }

    private static Result calculate(List<DayStatus> days, LocalDate today, YearMonth fullMonthToCheck) {
        return calculate(days, today, YearMonth.from(today), fullMonthToCheck);
    }

    private static Result calculate(List<DayStatus> days, LocalDate today, YearMonth currentMonth, YearMonth fullMonthToCheck) {
        return StreakCalculator.calculate(new Input(days, today, currentMonth, fullMonthToCheck));
    }

}
