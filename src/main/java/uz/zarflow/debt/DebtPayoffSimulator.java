package uz.zarflow.debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Projects a single debt forward month by month, with an optional extra-monthly top-up on
 * the regular payment, to answer "what if I paid {@code extraMonthly} more each month?" Pure
 * and Spring-free so it's unit-testable without a context.
 */
public final class DebtPayoffSimulator {

    /** Safety valve against a payment that never covers interest (would never close). */
    private static final int MAX_MONTHS = 1200;

    private DebtPayoffSimulator() {
    }

    public static Result simulate(Input input) {
        Projection baseline = project(input, BigDecimal.ZERO);
        Projection scenario = project(input, input.extraMonthly());

        int monthsSaved = baseline.months() - scenario.months();
        BigDecimal interestSaved = baseline.totalInterest().subtract(scenario.totalInterest());
        return new Result(scenario.closeDate(), monthsSaved, interestSaved);
    }

    static Projection project(Input input, BigDecimal extraMonthly) {
        BigDecimal outstanding = input.outstandingPrincipal();
        BigDecimal totalInterest = BigDecimal.ZERO;
        LocalDate previousDate = input.fromDate();
        LocalDate closeDate = previousDate;
        int months = 0;

        while (outstanding.signum() > 0 && months < MAX_MONTHS) {
            LocalDate dueDate = nextDueDate(previousDate, input.paymentDay());
            BigDecimal interest = interestFor(input, outstanding, previousDate, dueDate);
            BigDecimal payment = input.regularMonthlyPayment().add(extraMonthly);
            BigDecimal principal = round(payment.subtract(interest));
            if (principal.signum() < 0) {
                principal = BigDecimal.ZERO.setScale(2);
            }
            if (principal.compareTo(outstanding) > 0) {
                principal = outstanding;
            }

            outstanding = outstanding.subtract(principal);
            totalInterest = totalInterest.add(interest);
            months++;
            previousDate = dueDate;
            closeDate = dueDate;
        }

        return new Projection(months, totalInterest, closeDate);
    }

    private static BigDecimal interestFor(Input input, BigDecimal outstanding, LocalDate previousDate, LocalDate dueDate) {
        BigDecimal annualRate = input.annualRatePercent().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN);
        return switch (input.method()) {
            case ANNUITY_MONTHLY -> round(outstanding.multiply(annualRate).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN));
            case ACTUAL_365 -> {
                long days = ChronoUnit.DAYS.between(previousDate, dueDate);
                yield round(outstanding.multiply(annualRate).multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_EVEN));
            }
        };
    }

    private static LocalDate nextDueDate(LocalDate fromDate, int paymentDay) {
        YearMonth nextMonth = YearMonth.from(fromDate).plusMonths(1);
        int day = Math.min(paymentDay, nextMonth.lengthOfMonth());
        return nextMonth.atDay(day);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public record Input(
            BigDecimal outstandingPrincipal, BigDecimal annualRatePercent, InterestMethod method,
            BigDecimal regularMonthlyPayment, BigDecimal extraMonthly, LocalDate fromDate, int paymentDay) {
    }

    public record Result(LocalDate newCloseDate, int monthsSaved, BigDecimal interestSaved) {
    }

    record Projection(int months, BigDecimal totalInterest, LocalDate closeDate) {
    }

}
