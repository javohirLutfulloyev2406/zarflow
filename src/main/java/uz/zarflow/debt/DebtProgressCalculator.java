package uz.zarflow.debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** Paid %, months left, estimated close date, and remaining interest — read straight off the
 * debt's own stored outstanding balance and remaining (unpaid) schedule rows. Pure and
 * Spring-free so it's unit-testable without a context. */
public final class DebtProgressCalculator {

    private DebtProgressCalculator() {
    }

    public static Result calculate(Input input) {
        BigDecimal paidPercent = input.originalAmount().signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : input.originalAmount().subtract(input.outstandingPrincipal())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(input.originalAmount(), 2, RoundingMode.HALF_EVEN);

        List<Row> remaining = input.remainingSchedule();
        int monthsLeft = remaining.size();
        LocalDate estimatedCloseDate = remaining.isEmpty() ? null : remaining.get(remaining.size() - 1).dueDate();
        BigDecimal interestRemaining = remaining.stream()
                .map(row -> row.interest().add(row.fee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Result(paidPercent, monthsLeft, estimatedCloseDate, interestRemaining);
    }

    /** {@code remainingSchedule} must already be filtered to unpaid rows, ascending by due date. */
    public record Input(BigDecimal originalAmount, BigDecimal outstandingPrincipal, List<Row> remainingSchedule) {
    }

    public record Row(LocalDate dueDate, BigDecimal interest, BigDecimal fee) {
    }

    public record Result(BigDecimal paidPercent, int monthsLeft, LocalDate estimatedCloseDate, BigDecimal interestRemaining) {
    }

}
