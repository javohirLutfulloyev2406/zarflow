package uz.zarflow.recurring;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * The budget module's "fixed_payments/loan_payments auto" sums (docs/PRD.md M7 §3) --
 * pure, no Spring, composed directly on top of {@link RecurringDateCalculator}.
 */
public final class RecurringBudgetAutoSum {

    private static final int MAX_OCCURRENCES_GUARD = 1000;

    private RecurringBudgetAutoSum() {
    }

    public static Totals autoSum(List<RuleSnapshot> rules, YearMonth month) {
        return new Totals(sumKind(rules, RecurringKind.REGULAR, month), sumKind(rules, RecurringKind.LOAN, month));
    }

    /** {@code null} manualOverride means "auto"; a set value always wins. */
    public static BigDecimal resolve(BigDecimal manualOverride, BigDecimal autoValue) {
        return manualOverride != null ? manualOverride : autoValue;
    }

    private static BigDecimal sumKind(List<RuleSnapshot> rules, RecurringKind kind, YearMonth month) {
        BigDecimal total = BigDecimal.ZERO;
        for (RuleSnapshot rule : rules) {
            if (rule.kind() != kind || !rule.active() || rule.archivedAt() != null) {
                continue;
            }
            int occurrences = occurrencesInMonth(rule.schedule(), rule.startDate(), month);
            total = total.add(rule.amount().multiply(BigDecimal.valueOf(occurrences)));
        }
        return total;
    }

    private static int occurrencesInMonth(RecurringDateCalculator.Schedule schedule, LocalDate startDate, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate searchFrom = startDate.isAfter(monthStart) ? startDate : monthStart;
        LocalDate candidate = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, searchFrom);

        int count = 0;
        for (int guard = 0; guard < MAX_OCCURRENCES_GUARD && !candidate.isAfter(monthEnd); guard++) {
            count++;
            candidate = RecurringDateCalculator.nextAfter(schedule, candidate);
        }
        return count;
    }

    public record RuleSnapshot(
            RecurringKind kind, BigDecimal amount, boolean active, Instant archivedAt,
            RecurringDateCalculator.Schedule schedule, LocalDate startDate) {
    }

    public record Totals(BigDecimal fixedPayments, BigDecimal loanPayments) {
    }

}
