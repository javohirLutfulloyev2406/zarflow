package uz.zarflow.insight;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uz.zarflow.budget.BudgetCalculator;
import uz.zarflow.budget.Budgets;
import uz.zarflow.category.CategoryType;
import uz.zarflow.category.Categories;
import uz.zarflow.habit.Habits;
import uz.zarflow.shared.exception.NotFoundException;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@Service
class InsightsService implements Insights {

    private final InsightRepository repository;
    private final Transactions transactions;
    private final Budgets budgets;
    private final Categories categories;
    private final Habits habits;
    private final UserProfiles userProfiles;
    private final InsightTextRenderer renderer;
    private final ObjectMapper objectMapper;

    InsightsService(
            InsightRepository repository,
            Transactions transactions,
            Budgets budgets,
            Categories categories,
            Habits habits,
            UserProfiles userProfiles,
            InsightTextRenderer renderer,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.transactions = transactions;
        this.budgets = budgets;
        this.categories = categories;
        this.habits = habits;
        this.userProfiles = userProfiles;
        this.renderer = renderer;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void evaluateAndStore(UUID userId, LocalDate today) {
        InsightRuleEngine.Input input = buildInput(userId, today);
        String monthKey = YearMonth.from(today).toString();
        String weekKey = isoWeekKey(today);

        for (InsightRuleEngine.Candidate candidate : InsightRuleEngine.evaluate(input)) {
            String periodKey = candidate.code() == InsightCode.CATEGORY_UP_WEEK ? weekKey : monthKey;
            upsert(userId, candidate, periodKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveInsight> listActive(UUID userId, Locale locale) {
        return repository.findAllByUserIdAndDismissedAtIsNullOrderByPriorityAsc(userId).stream()
                .map(insight -> toActiveInsight(userId, insight, locale))
                .toList();
    }

    @Override
    @Transactional
    public void dismiss(UUID userId, UUID insightId) {
        Insight insight = repository.findByIdAndUserId(insightId, userId)
                .orElseThrow(() -> new NotFoundException("resource.not-found"));
        insight.dismiss();
        repository.save(insight);
    }

    @Override
    @Transactional
    public Optional<RenderedInsight> takeTopUnsentForBot(UUID userId, Locale locale) {
        return repository.findFirstByUserIdAndDismissedAtIsNullAndSentInBotAtIsNullOrderByPriorityAsc(userId)
                .map(insight -> {
                    insight.markSentInBot();
                    repository.save(insight);
                    String text = renderer.render(userId, insight.getCode(), fromJson(insight.getPayload()), locale);
                    return new RenderedInsight(insight.getId(), text);
                });
    }

    private void upsert(UUID userId, InsightRuleEngine.Candidate candidate, String periodKey) {
        // Never persist a language-specific category name -- it's resolved fresh at render time.
        Map<String, String> toStore = candidate.payload().entrySet().stream()
                .filter(e -> !"categoryName".equals(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
        String json = toJson(toStore);

        Optional<Insight> existing = repository.findByUserIdAndCodeAndPeriodKey(userId, candidate.code(), periodKey);
        if (existing.isPresent()) {
            if (!existing.get().isDismissed()) {
                existing.get().refresh(json, candidate.priority());
                repository.save(existing.get());
            }
            return;
        }
        repository.save(new Insight(userId, candidate.code(), periodKey, json, candidate.priority()));
    }

    private ActiveInsight toActiveInsight(UUID userId, Insight insight, Locale locale) {
        Map<String, String> payload = fromJson(insight.getPayload());
        return new ActiveInsight(
                insight.getId(), insight.getCode(), renderer.icon(insight.getCode()),
                renderer.render(userId, insight.getCode(), payload, locale),
                renderer.actionable(insight.getCode()), insight.getCreatedAt());
    }

    private InsightRuleEngine.Input buildInput(UUID userId, LocalDate today) {
        ZoneId zone = userProfiles.zoneOf(userId);
        YearMonth month = YearMonth.from(today);

        List<InsightRuleEngine.CategoryWeekChange> categoryChanges = categoryWeekChanges(userId, zone, today);

        Optional<Budgets.BudgetView> budget = budgets.getBudget(userId, month);
        List<Transactions.MonthEntry> monthEntries = transactions.monthEntries(userId, zone, month);

        BigDecimal freeBudget = null;
        BigDecimal spentSoFarThisMonth = null;
        BigDecimal forecastOverage = null;
        boolean forecastPositive = false;
        if (budget.isPresent()) {
            Budgets.BudgetView view = budget.get();
            freeBudget = view.freeBudget();
            spentSoFarThisMonth = spentNonFixedExpenseUpTo(monthEntries, zone, today);

            List<BudgetCalculator.Entry> entries = monthEntries.stream()
                    .map(e -> new BudgetCalculator.Entry(e.type(), e.amount(), e.isFixed(), e.transactionDate()))
                    .toList();
            BudgetCalculator.Result result = BudgetCalculator.calculate(new BudgetCalculator.Input(
                    nullToZero(view.expectedIncome()), nullToZero(view.fixedPayments()), nullToZero(view.loanPayments()),
                    nullToZero(view.plannedSavings()), entries, zone, today.atTime(12, 0).atZone(zone).toInstant()));
            forecastOverage = result.forecastOverage();
            forecastPositive = result.forecastPositive();
        }

        long daysLeftInMonth = BudgetCalculator.daysLeftInMonth(today, month);

        BigDecimal debtPayments = transactions.monthDebtPayments(userId, zone, month);
        BigDecimal monthIncome = sumByType(monthEntries, TransactionType.INCOME);

        BigDecimal discretionarySoFar = transactions
                .analyticsForRange(userId, zone, month.atDay(1), today)
                .discretionaryExpense();
        BigDecimal discretionarySamePeriodLastMonth = discretionaryLastMonthSamePeriod(userId, zone, month, today);

        int daysSinceLastActivity = daysSinceLastActivity(userId, today);

        return new InsightRuleEngine.Input(
                categoryChanges, freeBudget, spentSoFarThisMonth, daysLeftInMonth, forecastOverage, forecastPositive,
                debtPayments, monthIncome, discretionarySoFar, discretionarySamePeriodLastMonth, daysSinceLastActivity);
    }

    private List<InsightRuleEngine.CategoryWeekChange> categoryWeekChanges(UUID userId, ZoneId zone, LocalDate today) {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate thisWeekFrom = monday;
        LocalDate thisWeekTo = today;
        LocalDate lastWeekFrom = monday.minusDays(7);
        LocalDate lastWeekTo = monday.minusDays(1);

        Map<UUID, BigDecimal> thisWeek = categoryAmounts(userId, zone, thisWeekFrom, thisWeekTo);
        Map<UUID, BigDecimal> lastWeek = categoryAmounts(userId, zone, lastWeekFrom, lastWeekTo);

        Map<UUID, String> namesById = categories.listForUser(userId).stream()
                .filter(c -> c.type() == CategoryType.EXPENSE)
                .collect(Collectors.toMap(Categories.CategoryView::id, Categories.CategoryView::nameUz, (a, b) -> a));

        Set<UUID> allCategoryIds = new HashSet<>(thisWeek.keySet());
        allCategoryIds.addAll(lastWeek.keySet());

        return allCategoryIds.stream()
                .filter(namesById::containsKey)
                .map(id -> new InsightRuleEngine.CategoryWeekChange(
                        id, namesById.get(id),
                        thisWeek.getOrDefault(id, BigDecimal.ZERO), lastWeek.getOrDefault(id, BigDecimal.ZERO)))
                .toList();
    }

    private Map<UUID, BigDecimal> categoryAmounts(UUID userId, ZoneId zone, LocalDate from, LocalDate toInclusive) {
        return transactions.analyticsForRange(userId, zone, from, toInclusive).expenseByCategory().stream()
                .collect(Collectors.toMap(Transactions.CategoryAmount::categoryId, Transactions.CategoryAmount::amount));
    }

    private BigDecimal discretionaryLastMonthSamePeriod(UUID userId, ZoneId zone, YearMonth month, LocalDate today) {
        YearMonth previousMonth = month.minusMonths(1);
        int clampedDay = Math.min(today.getDayOfMonth(), previousMonth.lengthOfMonth());
        LocalDate previousTo = previousMonth.atDay(clampedDay);
        return transactions.analyticsForRange(userId, zone, previousMonth.atDay(1), previousTo).discretionaryExpense();
    }

    private int daysSinceLastActivity(UUID userId, LocalDate today) {
        Set<LocalDate> counted = habits.countedDatesBetween(userId, today.minusDays(30), today);
        for (int gap = 0; gap <= 30; gap++) {
            if (counted.contains(today.minusDays(gap))) {
                return gap;
            }
        }
        return 31;
    }

    private BigDecimal spentNonFixedExpenseUpTo(List<Transactions.MonthEntry> entries, ZoneId zone, LocalDate asOf) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transactions.MonthEntry entry : entries) {
            if (entry.type() != TransactionType.EXPENSE || entry.isFixed()) {
                continue;
            }
            LocalDate entryDate = entry.transactionDate().atZone(zone).toLocalDate();
            if (!entryDate.isAfter(asOf)) {
                total = total.add(entry.amount());
            }
        }
        return total;
    }

    private BigDecimal sumByType(List<Transactions.MonthEntry> entries, uz.zarflow.transaction.TransactionType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transactions.MonthEntry entry : entries) {
            if (entry.type() == type) {
                total = total.add(entry.amount());
            }
        }
        return total;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String isoWeekKey(LocalDate date) {
        int weekYear = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", weekYear, week);
    }

    private String toJson(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize insight payload", ex);
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize insight payload", ex);
        }
    }

}
