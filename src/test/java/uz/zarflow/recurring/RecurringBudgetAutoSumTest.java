package uz.zarflow.recurring;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.recurring.RecurringBudgetAutoSum.RuleSnapshot;
import uz.zarflow.recurring.RecurringBudgetAutoSum.Totals;
import uz.zarflow.recurring.RecurringDateCalculator.Schedule;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringBudgetAutoSumTest {

    private static final YearMonth NOVEMBER_2026 = YearMonth.of(2026, 11);

    @Test
    void autoSumGroupsByKindAndMultipliesByOccurrencesInTheMonth() {
        // Monthly rent (REGULAR, 1x in November) + weekly groceries (REGULAR, every Monday -> 4x
        // in November 2026, which has Mondays on 2/9/16/23/30) + a monthly loan installment (LOAN, 1x).
        RuleSnapshot rent = new RuleSnapshot(
                RecurringKind.REGULAR, new BigDecimal("2500000.00"), true, null,
                Schedule.monthly(5), LocalDate.of(2026, 1, 5));
        RuleSnapshot groceries = new RuleSnapshot(
                RecurringKind.REGULAR, new BigDecimal("100000.00"), true, null,
                Schedule.weekly(DayOfWeek.MONDAY), LocalDate.of(2026, 1, 5));
        RuleSnapshot loan = new RuleSnapshot(
                RecurringKind.LOAN, new BigDecimal("800000.00"), true, null,
                Schedule.monthly(20), LocalDate.of(2026, 1, 20));

        Totals totals = RecurringBudgetAutoSum.autoSum(List.of(rent, groceries, loan), NOVEMBER_2026);

        // 2,500,000 (rent x1) + 100,000 x 5 (Mondays: 2,9,16,23,30) = 3,000,000
        assertThat(totals.fixedPayments()).isEqualByComparingTo("3000000.00");
        assertThat(totals.loanPayments()).isEqualByComparingTo("800000.00");
    }

    @Test
    void manualOverrideWinsOverTheAutoSum() {
        BigDecimal manual = new BigDecimal("1234567.00");
        BigDecimal auto = new BigDecimal("9999999.00");

        assertThat(RecurringBudgetAutoSum.resolve(manual, auto)).isEqualByComparingTo(manual);
        assertThat(RecurringBudgetAutoSum.resolve(null, auto)).isEqualByComparingTo(auto);
    }

    @Test
    void archivedRuleIsExcludedFromTheAutoSumEvenIfArchivedMidMonth() {
        RuleSnapshot active = new RuleSnapshot(
                RecurringKind.REGULAR, new BigDecimal("500000.00"), true, null,
                Schedule.monthly(10), LocalDate.of(2026, 1, 10));
        RuleSnapshot archivedMidMonth = new RuleSnapshot(
                RecurringKind.REGULAR, new BigDecimal("500000.00"), false,
                LocalDate.of(2026, 11, 15).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                Schedule.monthly(10), LocalDate.of(2026, 1, 10));

        Totals totals = RecurringBudgetAutoSum.autoSum(List.of(active, archivedMidMonth), NOVEMBER_2026);

        assertThat(totals.fixedPayments()).isEqualByComparingTo("500000.00");
    }

    @Test
    void ruleStartingMidMonthDoesNotCountAnOccurrenceBeforeItStarted() {
        // Rule starts Nov 20; monthly due day is the 5th -> no November occurrence (the 5th already
        // passed before the rule existed), first due date is December 5th.
        RuleSnapshot lateStart = new RuleSnapshot(
                RecurringKind.REGULAR, new BigDecimal("300000.00"), true, null,
                Schedule.monthly(5), LocalDate.of(2026, 11, 20));

        Totals totals = RecurringBudgetAutoSum.autoSum(List.of(lateStart), NOVEMBER_2026);

        assertThat(totals.fixedPayments()).isEqualByComparingTo("0");
    }

    @Test
    void inactiveRuleIsExcludedFromTheAutoSum() {
        RuleSnapshot inactive = new RuleSnapshot(
                RecurringKind.LOAN, new BigDecimal("400000.00"), false, null,
                Schedule.monthly(1), LocalDate.of(2026, 1, 1));

        Totals totals = RecurringBudgetAutoSum.autoSum(List.of(inactive), NOVEMBER_2026);

        assertThat(totals.loanPayments()).isEqualByComparingTo("0");
    }

}
