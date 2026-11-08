package uz.zarflow.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import uz.zarflow.transaction.TransactionType;

/**
 * The "today's safe-to-spend" formula (docs/PRD.md §3, M6). Pure arithmetic and
 * local-day bucketing — no Spring, no I/O — so it's unit-testable without a context.
 * Every division keeps 6 fraction digits internally; only the final result is
 * rounded (HALF_EVEN) to money scale 2, so the days-left division chain doesn't
 * compound rounding error across intermediate steps.
 */
public final class BudgetCalculator {

    private static final int INTERMEDIATE_SCALE = 6;
    private static final int MONEY_SCALE = 2;

    private BudgetCalculator() {
    }

    public static Result calculate(Input input) {
        ZoneId zone = input.zone();
        LocalDate today = input.now().atZone(zone).toLocalDate();
        YearMonth month = YearMonth.from(today);

        BigDecimal actualIncome = sum(input.entries(), TransactionType.INCOME, false, zone, today, null);
        BigDecimal income = input.expectedIncome().max(actualIncome);
        BigDecimal freeBudget = income
                .subtract(input.fixedPayments())
                .subtract(input.loanPayments())
                .subtract(input.plannedSavings());

        BigDecimal spentBeforeToday = sum(input.entries(), TransactionType.EXPENSE, true, zone, today, Boundary.BEFORE);
        BigDecimal spentToday = sum(input.entries(), TransactionType.EXPENSE, true, zone, today, Boundary.ON);

        long daysLeft = daysLeftInMonth(today, month);
        long daysElapsed = daysElapsedInMonth(today, month);
        int daysInMonth = month.lengthOfMonth();

        BigDecimal remainingBeforeToday = freeBudget.subtract(spentBeforeToday);
        BigDecimal dailyAllowance = divide(remainingBeforeToday, daysLeft);
        BigDecimal todayLeftRaw = dailyAllowance.subtract(spentToday);
        boolean overspentToday = todayLeftRaw.signum() < 0;

        long daysLeftAfterToday = daysLeft - 1;
        BigDecimal remainingAfterToday = remainingBeforeToday.subtract(spentToday);
        BigDecimal tomorrowAllowance =
                daysLeftAfterToday > 0 ? round(divide(remainingAfterToday, daysLeftAfterToday)) : null;

        BigDecimal spentSoFar = spentBeforeToday.add(spentToday);
        BigDecimal projectedTotal = divide(spentSoFar, daysElapsed).multiply(BigDecimal.valueOf(daysInMonth));
        BigDecimal forecastOverage = round(projectedTotal.subtract(freeBudget));

        return new Result(
                round(dailyAllowance),
                round(spentToday),
                overspentToday ? BigDecimal.ZERO.setScale(MONEY_SCALE) : round(todayLeftRaw),
                overspentToday,
                overspentToday ? round(todayLeftRaw.negate()) : BigDecimal.ZERO.setScale(MONEY_SCALE),
                tomorrowAllowance,
                round(freeBudget),
                freeBudget.signum() < 0,
                forecastOverage,
                forecastOverage.signum() > 0);
    }

    /** Days from today to month-end, inclusive of today — 1 on the last day of the month. */
    public static long daysLeftInMonth(LocalDate today, YearMonth month) {
        return month.atEndOfMonth().toEpochDay() - today.toEpochDay() + 1;
    }

    private static long daysElapsedInMonth(LocalDate today, YearMonth month) {
        return today.toEpochDay() - month.atDay(1).toEpochDay() + 1;
    }

    private static BigDecimal sum(
            List<Entry> entries, TransactionType type, boolean excludeFixed, ZoneId zone, LocalDate today, Boundary boundary) {
        BigDecimal total = BigDecimal.ZERO;
        for (Entry entry : entries) {
            if (entry.type() != type) {
                continue;
            }
            if (excludeFixed && entry.isFixed()) {
                continue;
            }
            if (boundary != null) {
                LocalDate entryDate = entry.transactionDate().atZone(zone).toLocalDate();
                boolean matches = boundary == Boundary.BEFORE ? entryDate.isBefore(today) : entryDate.isEqual(today);
                if (!matches) {
                    continue;
                }
            }
            total = total.add(entry.amount());
        }
        return total;
    }

    private static BigDecimal divide(BigDecimal numerator, long denominator) {
        return numerator.divide(BigDecimal.valueOf(denominator), INTERMEDIATE_SCALE, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private enum Boundary {
        BEFORE, ON
    }

    /** TRANSFER entries are simply never matched by {@code TransactionType.INCOME}/{@code EXPENSE} and so never count. */
    public record Entry(TransactionType type, BigDecimal amount, boolean isFixed, Instant transactionDate) {
    }

    public record Input(
            BigDecimal expectedIncome,
            BigDecimal fixedPayments,
            BigDecimal loanPayments,
            BigDecimal plannedSavings,
            List<Entry> entries,
            ZoneId zone,
            Instant now) {
    }

    public record Result(
            BigDecimal dailyAllowance,
            BigDecimal spentToday,
            BigDecimal todayLeft,
            boolean overspentToday,
            BigDecimal overspentAmount,
            BigDecimal tomorrowAllowance,
            BigDecimal freeBudget,
            boolean obligationsExceedIncome,
            BigDecimal forecastOverage,
            boolean forecastPositive) {
    }

}
