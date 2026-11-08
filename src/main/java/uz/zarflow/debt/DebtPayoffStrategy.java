package uz.zarflow.debt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Compares Avalanche (highest rate first) vs Snowball (smallest balance first) across
 * several debts when extra money is available each month: whichever debt is "first" in that
 * priority order gets 100% of the extra, plus every already-closed debt's freed-up regular
 * payment; every other debt keeps paying only its own regular payment. Pure and Spring-free
 * so it's unit-testable without a context.
 *
 * <p>Priority order is fixed at the start of the run (snowball doesn't re-sort mid-simulation
 * as balances change) — a standard simplification for a payoff-strategy estimate, not a
 * month-by-month ledger.
 */
public final class DebtPayoffStrategy {

    private static final int MAX_MONTHS = 1200;

    private DebtPayoffStrategy() {
    }

    public static Comparison compare(List<DebtInput> debts, BigDecimal extraMonthly) {
        StrategyResult avalanche = simulate(debts, extraMonthly, Comparator.comparing(DebtInput::annualRatePercent).reversed());
        StrategyResult snowball = simulate(debts, extraMonthly, Comparator.comparing(DebtInput::outstandingPrincipal));
        Strategy recommended = avalanche.totalInterest().compareTo(snowball.totalInterest()) <= 0
                ? Strategy.AVALANCHE : Strategy.SNOWBALL;
        return new Comparison(avalanche, snowball, recommended);
    }

    private static StrategyResult simulate(List<DebtInput> debts, BigDecimal extraMonthly, Comparator<DebtInput> priority) {
        List<DebtInput> ordered = debts.stream().sorted(priority).toList();
        int n = ordered.size();
        BigDecimal[] outstanding = new BigDecimal[n];
        boolean[] closed = new boolean[n];
        for (int i = 0; i < n; i++) {
            outstanding[i] = ordered.get(i).outstandingPrincipal();
        }

        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal freedUp = BigDecimal.ZERO;
        LocalDate closeDate = ordered.get(0).fromDate();
        int months = 0;

        while (!allClosed(closed) && months < MAX_MONTHS) {
            months++;
            closeDate = closeDate.plusMonths(1);
            int targetIndex = firstOpenIndex(closed);

            for (int i = 0; i < n; i++) {
                if (closed[i]) {
                    continue;
                }
                DebtInput debt = ordered.get(i);
                BigDecimal monthlyRate = debt.annualRatePercent()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN)
                        .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);
                BigDecimal interest = round(outstanding[i].multiply(monthlyRate));
                BigDecimal extra = i == targetIndex ? extraMonthly.add(freedUp) : BigDecimal.ZERO;
                BigDecimal principal = round(debt.monthlyPayment().add(extra).subtract(interest));
                if (principal.signum() < 0) {
                    principal = BigDecimal.ZERO.setScale(2);
                }
                if (principal.compareTo(outstanding[i]) > 0) {
                    principal = outstanding[i];
                }

                outstanding[i] = outstanding[i].subtract(principal);
                totalInterest = totalInterest.add(interest);
                if (outstanding[i].signum() <= 0) {
                    closed[i] = true;
                    freedUp = freedUp.add(debt.monthlyPayment());
                }
            }
        }

        return new StrategyResult(closeDate, round(totalInterest));
    }

    private static boolean allClosed(boolean[] closed) {
        for (boolean c : closed) {
            if (!c) {
                return false;
            }
        }
        return true;
    }

    private static int firstOpenIndex(boolean[] closed) {
        for (int i = 0; i < closed.length; i++) {
            if (!closed[i]) {
                return i;
            }
        }
        return -1;
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public enum Strategy { AVALANCHE, SNOWBALL }

    public record DebtInput(
            UUID debtId, BigDecimal outstandingPrincipal, BigDecimal annualRatePercent, InterestMethod method,
            BigDecimal monthlyPayment, int paymentDay, LocalDate fromDate) {
    }

    public record StrategyResult(LocalDate closeDate, BigDecimal totalInterest) {
    }

    public record Comparison(StrategyResult avalanche, StrategyResult snowball, Strategy recommended) {
    }

}
