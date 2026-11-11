package uz.zarflow.debt;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Splits one debt payment into principal/interest/fee (PRD credit rules §3: principal is
 * never an expense, interest and fee always are). Pure and Spring-free so it's
 * unit-testable without a context.
 */
public final class PaymentSplitter {

    private static final int MONEY_SCALE = 2;

    private PaymentSplitter() {
    }

    public static Split split(Input input) {
        Split raw = rawSplit(input);
        return capPrincipalAtOutstanding(raw, input.outstandingPrincipal());
    }

    private static Split rawSplit(Input input) {
        if (input.extra()) {
            return new Split(input.amount(), BigDecimal.ZERO.setScale(MONEY_SCALE), BigDecimal.ZERO.setScale(MONEY_SCALE));
        }
        if (input.matchingRow() != null) {
            return new Split(input.matchingRow().principal(), input.matchingRow().interest(), input.matchingRow().fee());
        }
        // No schedule row to go by (manual-mode debt with an exhausted schedule, or a schedule
        // that was never generated) -- reconstruct one period's interest from the debt's own
        // method/rate on the current outstanding balance; whatever's left is principal.
        BigDecimal interest = interestByMethod(input);
        BigDecimal principal = input.amount().subtract(interest);
        if (principal.signum() < 0) {
            return new Split(BigDecimal.ZERO.setScale(MONEY_SCALE), input.amount(), BigDecimal.ZERO.setScale(MONEY_SCALE));
        }
        return new Split(round(principal), round(interest), BigDecimal.ZERO.setScale(MONEY_SCALE));
    }

    private static Split capPrincipalAtOutstanding(Split raw, BigDecimal outstandingPrincipal) {
        if (raw.principal().compareTo(outstandingPrincipal) <= 0) {
            return raw;
        }
        return new Split(outstandingPrincipal, raw.interest(), raw.fee());
    }

    private static BigDecimal interestByMethod(Input input) {
        BigDecimal annualRate = input.annualRatePercent().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN);
        return switch (input.method()) {
            case ANNUITY_MONTHLY -> round(input.outstandingPrincipal()
                    .multiply(annualRate)
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN));
            case ACTUAL_365 -> round(input.outstandingPrincipal()
                    .multiply(annualRate)
                    .multiply(BigDecimal.valueOf(input.daysInPeriod()))
                    .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_EVEN));
        };
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    /** {@code matchingRow} is the debt's next pending schedule row (if any) -- {@code null}
     * for a manual-mode debt with no generated/remaining rows. */
    public record Input(
            BigDecimal amount, boolean extra, BigDecimal outstandingPrincipal, ScheduleRow matchingRow,
            BigDecimal annualRatePercent, InterestMethod method, long daysInPeriod) {
    }

    public record ScheduleRow(BigDecimal principal, BigDecimal interest, BigDecimal fee) {
    }

    public record Split(BigDecimal principal, BigDecimal interest, BigDecimal fee) {
    }

}
