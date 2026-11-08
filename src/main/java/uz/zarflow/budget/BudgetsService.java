package uz.zarflow.budget;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.shared.common.Percentages;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@Service
class BudgetsService implements Budgets {

    private final BudgetRepository budgetRepository;
    private final BudgetLimitRepository budgetLimitRepository;
    private final Transactions transactions;
    private final UserProfiles userProfiles;
    private final Clock clock;

    BudgetsService(
            BudgetRepository budgetRepository,
            BudgetLimitRepository budgetLimitRepository,
            Transactions transactions,
            UserProfiles userProfiles,
            Clock clock) {
        this.budgetRepository = budgetRepository;
        this.budgetLimitRepository = budgetLimitRepository;
        this.transactions = transactions;
        this.userProfiles = userProfiles;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Optional<BudgetView> getBudget(UUID userId, YearMonth month) {
        Budget budget = resolveOrCopyForward(userId, month);
        if (budget == null) {
            return Optional.empty();
        }
        return Optional.of(toView(budget, spentByCategory(userId, month)));
    }

    @Override
    @Transactional
    public BudgetView updateBudget(UUID userId, YearMonth month, BudgetUpdate update) {
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month.atDay(1)).orElseGet(() -> new Budget(
                userId, month.atDay(1), update.expectedIncome(), update.fixedPayments(),
                update.loanPayments(), update.plannedSavings()));
        budget.update(update.expectedIncome(), update.fixedPayments(), update.loanPayments(), update.plannedSavings());
        budgetRepository.save(budget);

        budgetLimitRepository.deleteByBudgetId(budget.getId());
        for (LimitUpdate limitUpdate : update.limits()) {
            budgetLimitRepository.save(new BudgetLimit(budget.getId(), limitUpdate.categoryId(), limitUpdate.amount()));
        }
        return toView(budget, spentByCategory(userId, month));
    }

    @Override
    @Transactional
    public Optional<TodayView> getToday(UUID userId) {
        ZoneId zone = userProfiles.zoneOf(userId);
        YearMonth month = YearMonth.from(LocalDate.now(clock.withZone(zone)));
        Budget budget = resolveOrCopyForward(userId, month);
        if (budget == null) {
            return Optional.empty();
        }

        List<Transactions.MonthEntry> entries = transactions.monthEntries(userId, zone, month);
        List<BudgetCalculator.Entry> calcEntries = entries.stream()
                .map(e -> new BudgetCalculator.Entry(e.type(), e.amount(), e.isFixed(), e.transactionDate()))
                .toList();
        BudgetCalculator.Result result = BudgetCalculator.calculate(new BudgetCalculator.Input(
                budget.getExpectedIncome(), budget.getFixedPayments(), budget.getLoanPayments(), budget.getPlannedSavings(),
                calcEntries, zone, Instant.now(clock)));

        List<LimitView> limits = limitViews(budget, spentByCategory(entries));

        return Optional.of(new TodayView(
                result.dailyAllowance(), result.spentToday(), result.todayLeft(), result.overspentToday(),
                result.overspentAmount(), result.tomorrowAllowance(), result.freeBudget(),
                result.obligationsExceedIncome(), result.forecastOverage(), result.forecastPositive(), limits));
    }

    /** First access to a month with no budget row copies the previous month's budget + limits forward. */
    private Budget resolveOrCopyForward(UUID userId, YearMonth month) {
        Optional<Budget> existing = budgetRepository.findByUserIdAndMonth(userId, month.atDay(1));
        if (existing.isPresent()) {
            return existing.get();
        }
        Optional<Budget> previous = budgetRepository.findByUserIdAndMonth(userId, month.minusMonths(1).atDay(1));
        if (previous.isEmpty()) {
            return null;
        }
        Budget previousBudget = previous.get();
        Budget copy = new Budget(
                userId, month.atDay(1), previousBudget.getExpectedIncome(), previousBudget.getFixedPayments(),
                previousBudget.getLoanPayments(), previousBudget.getPlannedSavings());
        budgetRepository.save(copy);
        for (BudgetLimit limit : budgetLimitRepository.findByBudgetId(previousBudget.getId())) {
            budgetLimitRepository.save(new BudgetLimit(copy.getId(), limit.getCategoryId(), limit.getAmount()));
        }
        return copy;
    }

    private Map<UUID, BigDecimal> spentByCategory(UUID userId, YearMonth month) {
        ZoneId zone = userProfiles.zoneOf(userId);
        return spentByCategory(transactions.monthEntries(userId, zone, month));
    }

    private Map<UUID, BigDecimal> spentByCategory(List<Transactions.MonthEntry> entries) {
        return entries.stream()
                .filter(e -> e.type() == TransactionType.EXPENSE && e.categoryId() != null)
                .collect(Collectors.groupingBy(
                        Transactions.MonthEntry::categoryId, Collectors.reducing(BigDecimal.ZERO, Transactions.MonthEntry::amount, BigDecimal::add)));
    }

    private BudgetView toView(Budget budget, Map<UUID, BigDecimal> spentByCategory) {
        BigDecimal freeBudget = budget.getExpectedIncome()
                .subtract(budget.getFixedPayments())
                .subtract(budget.getLoanPayments())
                .subtract(budget.getPlannedSavings());
        return new BudgetView(
                YearMonth.from(budget.getMonth()), budget.getExpectedIncome(), budget.getFixedPayments(),
                budget.getLoanPayments(), budget.getPlannedSavings(), freeBudget, limitViews(budget, spentByCategory));
    }

    private List<LimitView> limitViews(Budget budget, Map<UUID, BigDecimal> spentByCategory) {
        return budgetLimitRepository.findByBudgetId(budget.getId()).stream()
                .map(limit -> {
                    BigDecimal spent = spentByCategory.getOrDefault(limit.getCategoryId(), BigDecimal.ZERO);
                    return new LimitView(limit.getCategoryId(), limit.getAmount(), spent, Percentages.shareOf(spent, limit.getAmount()));
                })
                .toList();
    }

}
