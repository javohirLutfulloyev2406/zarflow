package uz.zarflow.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.budget.BudgetCalculator.Entry;
import uz.zarflow.budget.BudgetCalculator.Input;
import uz.zarflow.budget.BudgetCalculator.Result;
import uz.zarflow.transaction.TransactionType;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetCalculatorTest {

    private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");

    @Test
    void normalDayComputesDailyAllowanceAndTodayLeft() {
        // Nov 2026 (30 days), today = Nov 21 -> daysLeft = 10. freeBudget = 6,000,000.
        List<Entry> entries = List.of(
                expense("1000000.00", false, at(2026, 11, 5, 12, 0)),
                expense("200000.00", false, at(2026, 11, 21, 12, 0)));
        Input input = new Input(
                new BigDecimal("8300000.00"), new BigDecimal("1000000.00"), new BigDecimal("300000.00"),
                new BigDecimal("1000000.00"), entries, TASHKENT, at(2026, 11, 21, 10, 0));

        Result result = BudgetCalculator.calculate(input);

        assertThat(result.freeBudget()).isEqualByComparingTo("6000000.00");
        assertThat(result.dailyAllowance()).isEqualByComparingTo("500000.00");
        assertThat(result.spentToday()).isEqualByComparingTo("200000.00");
        assertThat(result.todayLeft()).isEqualByComparingTo("300000.00");
        assertThat(result.overspentToday()).isFalse();
        assertThat(result.tomorrowAllowance()).isEqualByComparingTo("533333.33");
    }

    @Test
    void lastDayOfMonthHasDaysLeftOfOneAndNoTomorrowAllowance() {
        List<Entry> entries = List.of(
                expense("2000000.00", false, at(2026, 11, 15, 12, 0)),
                expense("100000.00", false, at(2026, 11, 30, 12, 0)));
        Input input = new Input(
                new BigDecimal("6000000.00"), new BigDecimal("1000000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                entries, TASHKENT, at(2026, 11, 30, 20, 0));

        assertThat(BudgetCalculator.daysLeftInMonth(LocalDate.of(2026, 11, 30), YearMonth.of(2026, 11))).isEqualTo(1);

        Result result = BudgetCalculator.calculate(input);

        assertThat(result.dailyAllowance()).isEqualByComparingTo("3000000.00");
        assertThat(result.todayLeft()).isEqualByComparingTo("2900000.00");
        assertThat(result.tomorrowAllowance()).isNull();
    }

    @Test
    void overspentTodayShowsZeroLeftPlusOverspentAmount() {
        // freeBudget 6,000,000 / 20 days left (Nov 11) = 300,000 daily allowance; today's 500,000 expense overspends it.
        List<Entry> entries = List.of(expense("500000.00", false, at(2026, 11, 11, 15, 0)));
        Input input = new Input(
                new BigDecimal("6000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                entries, TASHKENT, at(2026, 11, 11, 20, 0));

        Result result = BudgetCalculator.calculate(input);

        assertThat(result.dailyAllowance()).isEqualByComparingTo("300000.00");
        assertThat(result.overspentToday()).isTrue();
        assertThat(result.todayLeft()).isEqualByComparingTo("0.00");
        assertThat(result.overspentAmount()).isEqualByComparingTo("200000.00");
    }

    @Test
    void negativeFreeBudgetFlagsObligationsExceedIncome() {
        // income 3,000,000 - fixed 2,000,000 - loan 1,000,000 - savings 500,000 = -500,000.
        Input input = new Input(
                new BigDecimal("3000000.00"), new BigDecimal("2000000.00"), new BigDecimal("1000000.00"),
                new BigDecimal("500000.00"), List.of(), TASHKENT, at(2026, 11, 10, 10, 0));

        Result result = BudgetCalculator.calculate(input);

        assertThat(result.freeBudget()).isEqualByComparingTo("-500000.00");
        assertThat(result.obligationsExceedIncome()).isTrue();
        assertThat(result.overspentToday()).isTrue();
        assertThat(result.forecastPositive()).isTrue();
    }

    @Test
    void actualIncomeAboveExpectedIsUsedForFreeBudget() {
        // expectedIncome 5,000,000 but an actual 9,000,000 INCOME row this month -> income = max(...) = 9,000,000.
        List<Entry> entries = List.of(income("9000000.00", at(2026, 11, 3, 9, 0)));
        Input input = new Input(
                new BigDecimal("5000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                entries, TASHKENT, at(2026, 11, 10, 10, 0));

        Result result = BudgetCalculator.calculate(input);

        assertThat(result.freeBudget()).isEqualByComparingTo("9000000.00");
    }

    @Test
    void fixedExpensesAreExcludedFromSpend() {
        Input withoutFixed = new Input(
                new BigDecimal("6000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(expense("300000.00", false, at(2026, 11, 5, 12, 0))),
                TASHKENT, at(2026, 11, 10, 10, 0));
        Input withFixedAdded = new Input(
                new BigDecimal("6000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(
                        expense("300000.00", false, at(2026, 11, 5, 12, 0)),
                        expense("9999999.00", true, at(2026, 11, 6, 12, 0))),
                TASHKENT, at(2026, 11, 10, 10, 0));

        Result without = BudgetCalculator.calculate(withoutFixed);
        Result with = BudgetCalculator.calculate(withFixedAdded);

        assertThat(with.dailyAllowance()).isEqualByComparingTo(without.dailyAllowance());
        assertThat(with.freeBudget()).isEqualByComparingTo("6000000.00");
    }

    @Test
    void transfersAreExcludedFromSpend() {
        Input withoutTransfer = new Input(
                new BigDecimal("6000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(expense("300000.00", false, at(2026, 11, 5, 12, 0))),
                TASHKENT, at(2026, 11, 10, 10, 0));
        Input withTransferAdded = new Input(
                new BigDecimal("6000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(
                        expense("300000.00", false, at(2026, 11, 5, 12, 0)),
                        new Entry(TransactionType.TRANSFER, new BigDecimal("5000000.00"), false, at(2026, 11, 6, 12, 0))),
                TASHKENT, at(2026, 11, 10, 10, 0));

        Result without = BudgetCalculator.calculate(withoutTransfer);
        Result with = BudgetCalculator.calculate(withTransferAdded);

        assertThat(with.dailyAllowance()).isEqualByComparingTo(without.dailyAllowance());
    }

    @Test
    void dayBoundaryUsesUserTimezoneNotUtc() {
        // 23:30 Asia/Tashkent on Nov 9 is still Nov 9 in that zone (though already Nov 9 18:30Z in UTC too --
        // the point is the bucketing must go through the user's zone, not assume the entry's raw Instant date).
        Instant entryAt2330Local = LocalDate.of(2026, 11, 9).atTime(23, 30).atZone(TASHKENT).toInstant();
        List<Entry> entries = List.of(new Entry(TransactionType.EXPENSE, new BigDecimal("400000.00"), false, entryAt2330Local));
        Input baseInput = new Input(new BigDecimal("6000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                entries, TASHKENT, null);

        // "Now" is still the same local day (23:45) -> the 23:30 entry belongs to today.
        Result sameDay = BudgetCalculator.calculate(withNow(baseInput, at(2026, 11, 9, 23, 45)));
        assertThat(sameDay.spentToday()).isEqualByComparingTo("400000.00");
        assertThat(sameDay.overspentToday()).isTrue();

        // "Now" has rolled into the next local day (00:15) -> the 23:30 entry is now "before today", not "today".
        Result nextDay = BudgetCalculator.calculate(withNow(baseInput, at(2026, 11, 10, 0, 15)));
        assertThat(nextDay.spentToday()).isEqualByComparingTo("0.00");
        assertThat(nextDay.dailyAllowance()).isEqualByComparingTo("266666.67");
    }

    @Test
    void thirtyOneDayMonthUsesItsOwnLengthInForecast() {
        List<Entry> entries = List.of(expense("1000000.00", false, at(2026, 10, 5, 12, 0)));
        Input input = new Input(
                new BigDecimal("6200000.00"), new BigDecimal("200000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                entries, TASHKENT, at(2026, 10, 10, 10, 0));

        Result result = BudgetCalculator.calculate(input);

        assertThat(YearMonth.of(2026, 10).lengthOfMonth()).isEqualTo(31);
        assertThat(result.dailyAllowance()).isEqualByComparingTo("227272.73");
        assertThat(result.forecastOverage()).isEqualByComparingTo("-2900000.00");
    }

    @Test
    void leapFebruary2028HasTwentyNineDays() {
        List<Entry> entries = List.of(expense("500000.00", false, at(2028, 2, 5, 12, 0)));
        Input input = new Input(
                new BigDecimal("3000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                entries, TASHKENT, at(2028, 2, 10, 10, 0));

        Result result = BudgetCalculator.calculate(input);

        assertThat(YearMonth.of(2028, 2).lengthOfMonth()).isEqualTo(29);
        assertThat(result.dailyAllowance()).isEqualByComparingTo("125000.00");
        assertThat(result.forecastOverage()).isEqualByComparingTo("-1550000.00");
    }

    private static Entry expense(String amount, boolean isFixed, Instant date) {
        return new Entry(TransactionType.EXPENSE, new BigDecimal(amount), isFixed, date);
    }

    private static Entry income(String amount, Instant date) {
        return new Entry(TransactionType.INCOME, new BigDecimal(amount), false, date);
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day).atTime(hour, minute).atZone(TASHKENT).toInstant();
    }

    private static Input withNow(Input input, Instant now) {
        return new Input(
                input.expectedIncome(), input.fixedPayments(), input.loanPayments(), input.plannedSavings(),
                input.entries(), input.zone(), now);
    }

}
