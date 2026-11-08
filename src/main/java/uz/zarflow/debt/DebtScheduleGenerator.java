package uz.zarflow.debt;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds an amortization schedule for a debt. Pure and Spring-free so it's unit-testable
 * without a context. Two interest accruals share the same amortization shape (a constant
 * target monthly payment, principal = payment − interest) and differ only in how interest
 * for a row is computed:
 * <ul>
 *   <li>{@code ANNUITY_MONTHLY}: interest = outstanding × (annualRate/12).</li>
 *   <li>{@code ACTUAL_365}: interest = outstanding × annualRate × daysInPeriod / 365.</li>
 * </ul>
 * Every row is rounded to money scale (HALF_EVEN) as it's produced; the LAST row's principal
 * is always set to whatever principal remains, so the rows sum to {@code originalAmount}
 * exactly regardless of rounding drift in the earlier rows.
 */
public final class DebtScheduleGenerator {

    private static final int MONEY_SCALE = 2;
    private static final MathContext PRECISE = new MathContext(20);

    private DebtScheduleGenerator() {
    }

    public static Result generate(Input input) {
        BigDecimal monthlyRate = input.annualRatePercent()
                .divide(BigDecimal.valueOf(100), PRECISE)
                .divide(BigDecimal.valueOf(12), PRECISE);
        BigDecimal targetPayment = targetMonthlyPayment(input.originalAmount(), monthlyRate, input.termMonths());

        List<Row> rows = new ArrayList<>();
        BigDecimal outstanding = input.originalAmount();
        LocalDate previousDate = input.startDate();

        for (int seq = 1; seq <= input.termMonths(); seq++) {
            LocalDate dueDate = dueDateFor(input.startDate(), input.paymentDay(), seq);
            boolean isLast = seq == input.termMonths();

            BigDecimal interest = interestFor(input.method(), outstanding, input.annualRatePercent(), monthlyRate, previousDate, dueDate);
            BigDecimal principal = isLast ? outstanding : round(targetPayment.subtract(interest));
            if (principal.compareTo(outstanding) > 0) {
                principal = outstanding;
            }

            rows.add(new Row(seq, dueDate, round(principal), interest, BigDecimal.ZERO.setScale(MONEY_SCALE)));
            outstanding = outstanding.subtract(principal).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
            previousDate = dueDate;
        }

        return new Result(round(targetPayment), rows);
    }

    private static BigDecimal targetMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), PRECISE);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal discountFactor = BigDecimal.ONE.subtract(pow(onePlusR, -termMonths));
        return principal.multiply(monthlyRate, PRECISE).divide(discountFactor, PRECISE);
    }

    private static BigDecimal interestFor(
            InterestMethod method, BigDecimal outstanding, BigDecimal annualRatePercent, BigDecimal monthlyRate,
            LocalDate previousDate, LocalDate dueDate) {
        return switch (method) {
            case ANNUITY_MONTHLY -> round(outstanding.multiply(monthlyRate, PRECISE));
            case ACTUAL_365 -> {
                long days = java.time.temporal.ChronoUnit.DAYS.between(previousDate, dueDate);
                BigDecimal annualRate = annualRatePercent.divide(BigDecimal.valueOf(100), PRECISE);
                yield round(outstanding.multiply(annualRate, PRECISE).multiply(BigDecimal.valueOf(days), PRECISE)
                        .divide(BigDecimal.valueOf(365), PRECISE));
            }
        };
    }

    private static LocalDate dueDateFor(LocalDate startDate, int paymentDay, int seq) {
        YearMonth month = YearMonth.from(startDate).plusMonths(seq);
        int day = Math.min(paymentDay, month.lengthOfMonth());
        return month.atDay(day);
    }

    /** {@code BigDecimal} has no built-in integer-exponent power with a {@link MathContext},
     * so a negative exponent (needed for the annuity discount factor) is done by hand. */
    private static BigDecimal pow(BigDecimal base, int exponent) {
        if (exponent >= 0) {
            return base.pow(exponent, PRECISE);
        }
        return BigDecimal.ONE.divide(base.pow(-exponent, PRECISE), PRECISE);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    public record Input(
            BigDecimal originalAmount, BigDecimal annualRatePercent, InterestMethod method, int termMonths,
            LocalDate startDate, int paymentDay) {
    }

    public record Row(int seq, LocalDate dueDate, BigDecimal principal, BigDecimal interest, BigDecimal fee) {
    }

    public record Result(BigDecimal monthlyPayment, List<Row> rows) {
    }

}
