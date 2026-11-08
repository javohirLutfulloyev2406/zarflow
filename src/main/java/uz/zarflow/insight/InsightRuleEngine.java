package uz.zarflow.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Six independent spending-habit rules over numbers the caller has already aggregated
 * (this module never touches a repository). Pure and Spring-free so it's unit-testable
 * without a context. Thresholds live in {@link InsightThresholds}.
 */
public final class InsightRuleEngine {

    /** Lower bucket = ranked first: positive news, then actionable nudges/warnings, then
     * plain-informational. Within CATEGORY_UP_WEEK's own bucket, bigger overages rank first. */
    private static final Map<InsightCode, Integer> RANK_BUCKET = Map.of(
            InsightCode.DISCRETIONARY_DOWN, 0,
            InsightCode.NO_ENTRIES_3_DAYS, 1,
            InsightCode.BUDGET_80, 2,
            InsightCode.FORECAST_OVER, 3,
            InsightCode.CATEGORY_UP_WEEK, 4,
            InsightCode.DEBT_SHARE, 5);

    private InsightRuleEngine() {
    }

    public static List<Candidate> evaluate(Input input) {
        List<Candidate> candidates = new ArrayList<>();

        for (CategoryWeekChange change : input.categoryWeekChanges()) {
            categoryUpWeek(change).ifPresent(candidates::add);
        }
        budget80(input).ifPresent(candidates::add);
        forecastOver(input).ifPresent(candidates::add);
        debtShare(input).ifPresent(candidates::add);
        discretionaryDown(input).ifPresent(candidates::add);
        noEntries(input).ifPresent(candidates::add);

        List<Candidate> ranked = candidates.stream()
                .sorted(Comparator
                        .comparingInt((Candidate c) -> RANK_BUCKET.get(c.code()))
                        .thenComparing((Candidate c) -> magnitudeOf(c), Comparator.reverseOrder()))
                .toList();

        List<Candidate> withPriority = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            withPriority.add(ranked.get(i).withPriority(i));
        }
        return withPriority;
    }

    private static BigDecimal magnitudeOf(Candidate candidate) {
        String amount = candidate.payload().get("diffAmount");
        return amount != null ? new BigDecimal(amount) : BigDecimal.ZERO;
    }

    private static Optional<Candidate> categoryUpWeek(CategoryWeekChange change) {
        if (change.lastWeek().signum() <= 0) {
            // No baseline to compare against -- "X% up from zero" isn't a meaningful claim.
            return Optional.empty();
        }
        BigDecimal diff = change.thisWeek().subtract(change.lastWeek());
        if (diff.signum() <= 0 || diff.compareTo(InsightThresholds.CATEGORY_UP_MIN_AMOUNT) < 0) {
            return Optional.empty();
        }
        BigDecimal percent = diff.multiply(BigDecimal.valueOf(100))
                .divide(change.lastWeek(), 4, RoundingMode.HALF_UP);
        if (percent.compareTo(InsightThresholds.CATEGORY_UP_PERCENT) < 0) {
            return Optional.empty();
        }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("categoryId", change.categoryId().toString());
        payload.put("categoryName", change.categoryName());
        payload.put("percent", roundToInt(percent));
        payload.put("diffAmount", diff.toPlainString());
        return Optional.of(new Candidate(InsightCode.CATEGORY_UP_WEEK, payload, 0));
    }

    private static Optional<Candidate> budget80(Input input) {
        BigDecimal freeBudget = input.freeBudget();
        if (freeBudget == null || freeBudget.signum() <= 0) {
            return Optional.empty();
        }
        if (input.daysLeftInMonth() < InsightThresholds.BUDGET_80_MIN_DAYS_LEFT) {
            return Optional.empty();
        }
        BigDecimal percent = input.spentSoFarThisMonth().multiply(BigDecimal.valueOf(100))
                .divide(freeBudget, 4, RoundingMode.HALF_UP);
        if (percent.compareTo(InsightThresholds.BUDGET_80_PERCENT) < 0) {
            return Optional.empty();
        }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("percentUsed", roundToInt(percent));
        payload.put("daysLeft", String.valueOf(input.daysLeftInMonth()));
        return Optional.of(new Candidate(InsightCode.BUDGET_80, payload, 0));
    }

    private static Optional<Candidate> forecastOver(Input input) {
        if (!input.forecastPositive() || input.forecastOverage() == null) {
            return Optional.empty();
        }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("overageAmount", input.forecastOverage().toPlainString());
        return Optional.of(new Candidate(InsightCode.FORECAST_OVER, payload, 0));
    }

    private static Optional<Candidate> debtShare(Input input) {
        if (input.monthIncome() == null || input.monthIncome().signum() <= 0 || input.debtPayments() == null) {
            return Optional.empty();
        }
        BigDecimal percent = input.debtPayments().multiply(BigDecimal.valueOf(100))
                .divide(input.monthIncome(), 4, RoundingMode.HALF_UP);
        if (percent.compareTo(InsightThresholds.DEBT_SHARE_PERCENT) < 0) {
            return Optional.empty();
        }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("percent", roundToInt(percent));
        return Optional.of(new Candidate(InsightCode.DEBT_SHARE, payload, 0));
    }

    private static Optional<Candidate> discretionaryDown(Input input) {
        if (input.discretionarySoFarThisMonth() == null || input.discretionarySamePeriodLastMonth() == null) {
            return Optional.empty();
        }
        BigDecimal diff = input.discretionarySamePeriodLastMonth().subtract(input.discretionarySoFarThisMonth());
        if (diff.signum() <= 0) {
            return Optional.empty();
        }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("diffAmount", diff.toPlainString());
        return Optional.of(new Candidate(InsightCode.DISCRETIONARY_DOWN, payload, 0));
    }

    private static Optional<Candidate> noEntries(Input input) {
        if (input.daysSinceLastActivity() < InsightThresholds.NO_ENTRIES_DAYS) {
            return Optional.empty();
        }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("days", String.valueOf(input.daysSinceLastActivity()));
        return Optional.of(new Candidate(InsightCode.NO_ENTRIES_3_DAYS, payload, 0));
    }

    private static String roundToInt(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    public record CategoryWeekChange(UUID categoryId, String categoryName, BigDecimal thisWeek, BigDecimal lastWeek) {
    }

    public record Input(
            List<CategoryWeekChange> categoryWeekChanges,
            BigDecimal freeBudget,
            BigDecimal spentSoFarThisMonth,
            long daysLeftInMonth,
            BigDecimal forecastOverage,
            boolean forecastPositive,
            BigDecimal debtPayments,
            BigDecimal monthIncome,
            BigDecimal discretionarySoFarThisMonth,
            BigDecimal discretionarySamePeriodLastMonth,
            int daysSinceLastActivity) {
    }

    /** {@code priority} is assigned by {@link #evaluate} -- 0 is the most important insight
     * in THIS evaluation; the raw rule output always starts at 0 before ranking. */
    public record Candidate(InsightCode code, Map<String, String> payload, int priority) {
        Candidate withPriority(int newPriority) {
            return new Candidate(code, payload, newPriority);
        }
    }

}
