package uz.zarflow.budget;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The budget module's public API. Other modules talk to budgets only through
 * here — never through {@code Budget}/{@code BudgetLimit}/their repositories.
 */
public interface Budgets {

    /** Empty only when neither this month nor the previous month has a budget to copy forward from. */
    Optional<BudgetView> getBudget(UUID userId, YearMonth month);

    /** Upserts the budget fields and replaces the limits list wholesale. */
    BudgetView updateBudget(UUID userId, YearMonth month, BudgetUpdate update);

    /** Empty when the user has no budget configured yet (not even a previous month to copy). */
    Optional<TodayView> getToday(UUID userId);

    record LimitView(UUID categoryId, BigDecimal amount, BigDecimal spent, BigDecimal percent) {
    }

    record BudgetView(
            YearMonth month,
            BigDecimal expectedIncome,
            BigDecimal fixedPayments,
            boolean fixedPaymentsIsAuto,
            BigDecimal loanPayments,
            boolean loanPaymentsIsAuto,
            BigDecimal plannedSavings,
            BigDecimal freeBudget,
            List<LimitView> limits) {
    }

    record LimitUpdate(UUID categoryId, BigDecimal amount) {
    }

    /** {@code null} fixedPayments/loanPayments means "go back to auto from recurring rules". */
    record BudgetUpdate(
            BigDecimal expectedIncome,
            BigDecimal fixedPayments,
            BigDecimal loanPayments,
            BigDecimal plannedSavings,
            List<LimitUpdate> limits) {
    }

    record TodayView(
            BigDecimal dailyAllowance,
            BigDecimal spentToday,
            BigDecimal todayLeft,
            boolean overspentToday,
            BigDecimal overspentAmount,
            BigDecimal tomorrowAllowance,
            BigDecimal freeBudget,
            boolean obligationsExceedIncome,
            BigDecimal forecastOverage,
            boolean forecastPositive,
            List<LimitView> limits) {
    }

    /** Published once per threshold per limit per month — the telegram module listens for this to send the alert. */
    record LimitThresholdCrossed(UUID userId, UUID categoryId, int thresholdPercent) {
    }

}
