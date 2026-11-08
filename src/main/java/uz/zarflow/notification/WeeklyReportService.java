package uz.zarflow.notification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.category.Categories;
import uz.zarflow.habit.Habits;
import uz.zarflow.shared.exception.NotFoundException;
import uz.zarflow.telegram.BotNotifications;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

/** The week-ending-today digest that stands in for the daily report every Sunday. */
@Service
class WeeklyReportService {

    private static final int TOP_CATEGORIES = 3;

    private final Transactions transactions;
    private final Categories categories;
    private final Habits habits;
    private final BotNotifications botNotifications;
    private final UserProfiles userProfiles;
    private final Clock clock;

    WeeklyReportService(
            Transactions transactions, Categories categories, Habits habits, BotNotifications botNotifications,
            UserProfiles userProfiles, Clock clock) {
        this.transactions = transactions;
        this.categories = categories;
        this.habits = habits;
        this.botNotifications = botNotifications;
        this.userProfiles = userProfiles;
        this.clock = clock;
    }

    /** For the local-only dev endpoint -- always sends, regardless of day-of-week. */
    @Transactional
    public void sendNow(UUID userId, LocalDate asOfDateOrNull) {
        UserProfiles.UserSummary user = userProfiles.find(userId).orElseThrow(() -> new NotFoundException("user.not_found"));
        ZoneId zone = user.timezone();
        LocalDate date = asOfDateOrNull != null ? asOfDateOrNull : LocalDate.now(clock.withZone(zone));
        sendWeekly(user, zone, date);
    }

    @Transactional(readOnly = true)
    public void sendWeekly(UserProfiles.UserSummary user, ZoneId zone, LocalDate weekEnding) {
        LocalDate from = weekEnding.minusDays(6);
        Transactions.PeriodAnalytics thisWeek = transactions.analyticsForRange(user.id(), zone, from, weekEnding);
        BigDecimal total = thisWeek.expense();
        BigDecimal dailyAverage = total.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_EVEN);

        LocalDate highestDay = null;
        BigDecimal highestAmount = BigDecimal.ZERO;
        for (LocalDate d = from; !d.isAfter(weekEnding); d = d.plusDays(1)) {
            BigDecimal dayExpense = transactions.dailyDigest(user.id(), zone, d).expense();
            if (dayExpense.compareTo(highestAmount) > 0) {
                highestAmount = dayExpense;
                highestDay = d;
            }
        }

        LocalDate lastWeekFrom = from.minusDays(7);
        LocalDate lastWeekTo = from.minusDays(1);
        BigDecimal lastWeekExpense = transactions.analyticsForRange(user.id(), zone, lastWeekFrom, lastWeekTo).expense();
        BigDecimal changePercent = lastWeekExpense.signum() > 0
                ? total.subtract(lastWeekExpense).multiply(BigDecimal.valueOf(100))
                        .divide(lastWeekExpense, 0, RoundingMode.HALF_UP)
                : null;

        List<BotNotifications.CategoryShare> topCategories = topCategories(user.id(), user.language(), thisWeek);
        int streakDays = habits.getSummary(user.id()).currentStreak();

        botNotifications.sendWeeklyReport(user.telegramId(), user.language(), new BotNotifications.WeeklyReportActivity(
                from, weekEnding, total, dailyAverage, highestDay,
                highestDay == null ? null : highestAmount, topCategories, changePercent, streakDays));
    }

    private List<BotNotifications.CategoryShare> topCategories(
            UUID userId, String language, Transactions.PeriodAnalytics analytics) {
        boolean russian = "ru".equals(language);
        Map<UUID, String> namesById = categories.listForUser(userId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Categories.CategoryView::id, c -> russian ? c.nameRu() : c.nameUz(), (a, b) -> a));
        return analytics.expenseByCategory().stream()
                .sorted(Comparator.comparing(Transactions.CategoryAmount::amount).reversed())
                .limit(TOP_CATEGORIES)
                .filter(c -> namesById.containsKey(c.categoryId()))
                .map(c -> new BotNotifications.CategoryShare(namesById.get(c.categoryId()), c.amount()))
                .toList();
    }

}
