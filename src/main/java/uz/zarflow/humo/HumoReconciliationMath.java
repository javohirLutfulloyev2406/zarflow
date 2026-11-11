package uz.zarflow.humo;

import java.math.BigDecimal;

/**
 * Pure diff math for one card (docs/PRD.md §7): a difference under 1 so'm in absolute
 * value counts as matching (rounding noise, not a real mismatch). No Spring -- unit-testable.
 */
final class HumoReconciliationMath {

    private static final BigDecimal MATCH_THRESHOLD = BigDecimal.ONE;

    private HumoReconciliationMath() {
    }

    static Classification classify(BigDecimal bankBalance, BigDecimal zarflowBalance) {
        BigDecimal diff = bankBalance.subtract(zarflowBalance);
        boolean matches = diff.abs().compareTo(MATCH_THRESHOLD) < 0;
        return new Classification(diff, matches);
    }

    /** {@code diff = bankBalance - zarflowBalance}: positive means ZarFlow is under-counting
     * (an unlogged income or missed expense reduces an outflow), negative means over-counting. */
    record Classification(BigDecimal diff, boolean matches) {
    }

}