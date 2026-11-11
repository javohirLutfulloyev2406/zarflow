package uz.zarflow.savings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Remaining amount, pace of saving, and on-track/forecast math for one savings goal. Pure
 * and Spring-free so it's unit-testable without a context. Per-period amounts ("you need
 * X per month") are rounded UP to a whole so'm — under-saving by a tiyin because of
 * rounding would mean missing the target date.
 */
public final class SavingsGoalCalculator {

    private static final int MONEY_SCALE = 2;

    private SavingsGoalCalculator() {
    }

    public static Result calculate(Input input) {
        boolean achieved = input.savedAmount().compareTo(input.targetAmount()) >= 0;
        BigDecimal remaining = achieved
                ? BigDecimal.ZERO.setScale(MONEY_SCALE)
                : input.targetAmount().subtract(input.savedAmount()).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

        BigDecimal pace = average(input.recentMonthlyContributions());

        BigDecimal perMonth = null;
        BigDecimal perWeek = null;
        if (!achieved && input.targetDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(input.today(), input.targetDate());
            long monthsLeft = daysLeft <= 0 ? 0 : ceilDiv(daysLeft, 30);
            long weeksLeft = daysLeft <= 0 ? 0 : ceilDiv(daysLeft, 7);
            perMonth = monthsLeft <= 0 ? remaining : ceilToWholeUnit(remaining, monthsLeft);
            perWeek = weeksLeft <= 0 ? remaining : ceilToWholeUnit(remaining, weeksLeft);
        }

        LocalDate forecastDate;
        if (achieved) {
            forecastDate = input.today();
        } else if (pace.signum() <= 0) {
            forecastDate = null;
        } else {
            long monthsNeeded = ceilDiv(remaining, pace);
            forecastDate = input.today().plusMonths(monthsNeeded);
        }

        boolean onTrack;
        if (achieved) {
            onTrack = true;
        } else if (input.targetDate() == null) {
            onTrack = pace.signum() > 0;
        } else {
            onTrack = forecastDate != null && !forecastDate.isAfter(input.targetDate());
        }

        return new Result(remaining, achieved, perMonth, perWeek, round(pace), forecastDate, onTrack);
    }

    private static BigDecimal average(List<BigDecimal> recentMonthlyContributions) {
        if (recentMonthlyContributions.isEmpty()) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE);
        }
        BigDecimal sum = recentMonthlyContributions.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(recentMonthlyContributions.size()), MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    /** Rounds {@code amount} up to the nearest whole so'm after dividing by {@code periods}. */
    private static BigDecimal ceilToWholeUnit(BigDecimal amount, long periods) {
        return amount.divide(BigDecimal.valueOf(periods), 0, RoundingMode.CEILING).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    private static long ceilDiv(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal quotient = numerator.divide(denominator, 0, RoundingMode.CEILING);
        return Math.max(1, quotient.longValueExact());
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    public record Input(
            BigDecimal targetAmount, BigDecimal savedAmount, LocalDate targetDate, LocalDate today,
            List<BigDecimal> recentMonthlyContributions) {
    }

    public record Result(
            BigDecimal remaining, boolean achieved, BigDecimal perMonth, BigDecimal perWeek,
            BigDecimal pace, LocalDate forecastDate, boolean onTrack) {
    }

}
