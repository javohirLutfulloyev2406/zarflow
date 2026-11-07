package uz.zarflow.analytics;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.category.Categories;
import uz.zarflow.shared.common.Percentages;
import uz.zarflow.shared.security.CurrentUser;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@RestController
@RequestMapping("/api/v1/analytics")
class AnalyticsController {

    private final Transactions transactions;
    private final Categories categories;
    private final UserProfiles userProfiles;
    private final CurrentUser currentUser;
    private final Clock clock;

    AnalyticsController(Transactions transactions, Categories categories, UserProfiles userProfiles, CurrentUser currentUser, Clock clock) {
        this.transactions = transactions;
        this.categories = categories;
        this.userProfiles = userProfiles;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping("/month")
    MonthResponse month(@RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        UUID userId = currentUser.requireUserId();
        ZoneId zone = userProfiles.zoneOf(userId);
        YearMonth previousMonth = month.minusMonths(1);

        Transactions.PeriodAnalytics current = transactions.analyticsForRange(userId, zone, month.atDay(1), month.atEndOfMonth());
        Transactions.PeriodAnalytics previous =
                transactions.analyticsForRange(userId, zone, previousMonth.atDay(1), previousMonth.atEndOfMonth());

        Map<UUID, Categories.CategoryView> categoryById =
                categories.listForUser(userId).stream().collect(Collectors.toMap(Categories.CategoryView::id, c -> c));
        Map<UUID, BigDecimal> previousByCategory = previous.expenseByCategory().stream()
                .collect(Collectors.toMap(Transactions.CategoryAmount::categoryId, Transactions.CategoryAmount::amount));

        boolean russian = isRussian();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        int days = AnalyticsMath.elapsedOrFullDays(month, today);

        List<CategoryBreakdownResponse> categoryResponses = current.expenseByCategory().stream()
                .sorted(Comparator.comparing(Transactions.CategoryAmount::amount).reversed())
                .map(ca -> {
                    Categories.CategoryView view = categoryById.get(ca.categoryId());
                    BigDecimal previousAmount = previousByCategory.get(ca.categoryId());
                    return new CategoryBreakdownResponse(
                            ca.categoryId(),
                            view == null ? "" : (russian ? view.nameRu() : view.nameUz()),
                            view == null ? null : view.color(),
                            view == null ? null : view.icon(),
                            ca.amount(),
                            Percentages.shareOf(ca.amount(), current.expense()),
                            Percentages.changeOf(ca.amount(), previousAmount));
                })
                .toList();

        return new MonthResponse(
                current.income(), current.expense(), current.income().subtract(current.expense()),
                previous.expense(), Percentages.changeOf(current.expense(), previous.expense()),
                current.essentialExpense(), current.discretionaryExpense(),
                AnalyticsMath.dailyAverage(current.expense(), days),
                categoryResponses);
    }

    @GetMapping("/calendar")
    CalendarResponse calendar(@RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        UUID userId = currentUser.requireUserId();
        ZoneId zone = userProfiles.zoneOf(userId);

        List<DayTotalResponse> days = transactions.dailyTotalsForMonth(userId, zone, month).stream()
                .map(d -> new DayTotalResponse(d.date(), d.income(), d.expense(), d.count()))
                .toList();
        return new CalendarResponse(days);
    }

    @GetMapping("/day")
    DayResponse day(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID userId = currentUser.requireUserId();
        ZoneId zone = userProfiles.zoneOf(userId);
        Transactions.DayDetail detail = transactions.dayDetail(userId, zone, date);

        List<TransactionResponse> items = detail.transactions().stream()
                .map(t -> new TransactionResponse(t.id(), t.accountId(), t.categoryId(), t.type(), t.amount(), t.description(), t.transactionDate()))
                .toList();
        return new DayResponse(items, detail.income(), detail.expense());
    }

    private boolean isRussian() {
        return "ru".equals(LocaleContextHolder.getLocale().getLanguage());
    }

    record CategoryBreakdownResponse(
            UUID categoryId, String name, String color, String icon,
            BigDecimal amount, BigDecimal sharePercent, BigDecimal changeVsPrevMonthPercent) {
    }

    record MonthResponse(
            BigDecimal income, BigDecimal expense, BigDecimal net,
            BigDecimal previousMonthExpense, BigDecimal expenseChangeVsPrevMonthPercent,
            BigDecimal essentialExpense, BigDecimal discretionaryExpense,
            BigDecimal dailyAverageExpense,
            List<CategoryBreakdownResponse> categories) {
    }

    record DayTotalResponse(LocalDate date, BigDecimal income, BigDecimal expense, int count) {
    }

    record CalendarResponse(List<DayTotalResponse> days) {
    }

    record TransactionResponse(
            UUID id, UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount, String description,
            Instant transactionDate) {
    }

    record DayResponse(List<TransactionResponse> transactions, BigDecimal income, BigDecimal expense) {
    }

}
