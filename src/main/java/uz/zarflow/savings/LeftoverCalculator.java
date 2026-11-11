package uz.zarflow.savings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import uz.zarflow.transaction.TransactionType;

/**
 * The month-end leftover (PRD S6): free budget minus that month's counted spending, using
 * the exact same inclusion rule as the daily safe-to-spend limit -- non-fixed EXPENSE only,
 * transfers and fixed/recurring rows never count. Pure and Spring-free so it's
 * unit-testable without a context.
 */
public final class LeftoverCalculator {

    private static final int MONEY_SCALE = 2;

    private LeftoverCalculator() {
    }

    public static Result calculate(Input input) {
        BigDecimal spent = input.entries().stream()
                .filter(e -> e.type() == TransactionType.EXPENSE && !e.isFixed())
                .map(Entry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal leftover = input.freeBudget().subtract(spent).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        boolean positive = leftover.signum() > 0;
        return new Result(positive ? leftover : BigDecimal.ZERO.setScale(MONEY_SCALE), positive);
    }

    public record Entry(TransactionType type, BigDecimal amount, boolean isFixed) {
    }

    public record Input(BigDecimal freeBudget, List<Entry> entries) {
    }

    public record Result(BigDecimal leftover, boolean positive) {
    }

}
