package uz.zarflow.notification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.budget.Budgets;
import uz.zarflow.category.Categories;
import uz.zarflow.savings.Savings;
import uz.zarflow.shared.exception.NotFoundException;
import uz.zarflow.telegram.BotNotifications;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

/** The 1st-of-the-month, 10:00-local report covering the just-completed previous month. */
@Service
class MonthlyReportService {

    static final LocalTime REPORT_TIME = LocalTime.of(10, 0);
    private static final int TOP_CATEGORIES = 5;

    private final UserProfiles userProfiles;
    private final NotificationSettingsRepository settingsRepository;
    private final Transactions transactions;
    private final Categories categories;
    private final Budgets budgets;
    private final Savings savings;
    private final BotNotifications botNotifications;
    private final Clock clock;

    MonthlyReportService(
            UserProfiles userProfiles,
            NotificationSettingsRepository settingsRepository,
            Transactions transactions,
            Categories categories,
            Budgets budgets,
            Savings savings,
            BotNotifications botNotifications,
            Clock clock) {
        this.userProfiles = userProfiles;
        this.settingsRepository = settingsRepository;
        this.transactions = transactions;
        this.categories = categories;
        this.budgets = budgets;
        this.savings = savings;
        this.botNotifications = botNotifications;
        this.clock = clock;
    }

    /** Public: proxy-based {@code @Transactional} AOP only intercepts public methods. */
    @Transactional
    public void runDueReports() {
        for (UserProfiles.UserSummary user : userProfiles.findAllActive()) {
            if (user.telegramId() == null) {
                continue;
            }
            processUser(user);
        }
    }

    private void processUser(UserProfiles.UserSummary user) {
        ZoneId zone = user.timezone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalTime nowLocal = LocalTime.now(clock.withZone(zone));

        NotificationSettings settings = settingsRepository.findById(user.id()).orElse(null);
        boolean enabled = settings == null || settings.isMonthlyReport();
        YearMonth reportedMonth = YearMonth.from(today).minusMonths(1);
        YearMonth lastReported = settings == null ? null : settings.getLastMonthlyReportOn();

        boolean dueByDate = today.getDayOfMonth() == 1;
        boolean dueByTime = !nowLocal.isBefore(REPORT_TIME);
        boolean alreadySent = lastReported != null && !lastReported.isBefore(reportedMonth);
        if (!enabled || !dueByDate || !dueByTime || alreadySent) {
            return;
        }

        sendMonthly(user, zone, reportedMonth);
        savings.evaluateLeftover(user.id(), reportedMonth);

        NotificationSettings toSave = settings == null ? new NotificationSettings(user.id()) : settings;
        toSave.markMonthlySent(reportedMonth);
        settingsRepository.save(toSave);
    }

    /** For the local-only dev endpoint -- ignores the "1st of the month, 10:00, not already
     * sent" gate and reports on the month {@code asOfDate} falls in (not the month before). */
    @Transactional
    public void sendNow(UUID userId, LocalDate asOfDateOrNull) {
        UserProfiles.UserSummary user = userProfiles.find(userId).orElseThrow(() -> new NotFoundException("user.not_found"));
        ZoneId zone = user.timezone();
        LocalDate date = asOfDateOrNull != null ? asOfDateOrNull : LocalDate.now(clock.withZone(zone));
        sendMonthly(user, zone, YearMonth.from(date));
    }

    private void sendMonthly(UserProfiles.UserSummary user, ZoneId zone, YearMonth month) {
        List<Transactions.MonthEntry> entries = transactions.monthEntries(user.id(), zone, month);
        BigDecimal income = sumByType(entries, TransactionType.INCOME);
        BigDecimal expense = sumByType(entries, TransactionType.EXPENSE);
        BigDecimal debtPayments = transactions.monthDebtPayments(user.id(), zone, month);
        BigDecimal net = income.subtract(expense);

        BigDecimal savings = budgets.getBudget(user.id(), month).map(Budgets.BudgetView::plannedSavings).orElse(null);

        List<BotNotifications.CategoryShare> topCategories = topCategories(user.id(), user.language(), entries);

        YearMonth previousMonth = month.minusMonths(1);
        BigDecimal previousExpense = sumByType(transactions.monthEntries(user.id(), zone, previousMonth), TransactionType.EXPENSE);
        BigDecimal changePercent = previousExpense.signum() > 0
                ? expense.subtract(previousExpense).multiply(BigDecimal.valueOf(100))
                        .divide(previousExpense, 0, RoundingMode.HALF_UP)
                : null;

        Savings.MonthSavingsSummary goalSavings = this.savings.monthSummary(user.id(), zone, month);

        botNotifications.sendMonthlyReport(user.telegramId(), user.language(), new BotNotifications.MonthlyReportActivity(
                month, income, expense, net, debtPayments, savings, topCategories, changePercent,
                goalSavings.totalSaved(), goalSavings.percentOfIncome()));
    }

    private List<BotNotifications.CategoryShare> topCategories(UUID userId, String language, List<Transactions.MonthEntry> entries) {
        boolean russian = "ru".equals(language);
        Map<UUID, String> namesById = categories.listForUser(userId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Categories.CategoryView::id, c -> russian ? c.nameRu() : c.nameUz(), (a, b) -> a));
        Map<UUID, BigDecimal> byCategory = entries.stream()
                .filter(e -> e.type() == TransactionType.EXPENSE && e.categoryId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        Transactions.MonthEntry::categoryId,
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, Transactions.MonthEntry::amount, BigDecimal::add)));
        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(TOP_CATEGORIES)
                .filter(e -> namesById.containsKey(e.getKey()))
                .map(e -> new BotNotifications.CategoryShare(namesById.get(e.getKey()), e.getValue()))
                .toList();
    }

    private BigDecimal sumByType(List<Transactions.MonthEntry> entries, TransactionType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transactions.MonthEntry entry : entries) {
            if (entry.type() == type) {
                total = total.add(entry.amount());
            }
        }
        return total;
    }

}
