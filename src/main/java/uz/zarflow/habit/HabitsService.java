package uz.zarflow.habit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.budget.BudgetCalculator;
import uz.zarflow.budget.Budgets;
import uz.zarflow.habit.StreakCalculator.DayStatus;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@Service
class HabitsService implements Habits {

    /** Lookback for streaks/FULL_MONTH -- plain reads (no side effects), so a wide window is
     * cheap and safe. withinLimit reconstruction is scoped to the CURRENT month only (see
     * {@link #buildDays}): Budgets.getBudget copy-forwards a budget row the first time a month
     * is queried, and this service must never backfill a budget the user never configured
     * just because a background sweep happened to look at an old month. */
    private static final int LOOKBACK_DAYS = 60;

    private final DayMarkRepository dayMarkRepository;
    private final UserAchievementRepository achievementRepository;
    private final Transactions transactions;
    private final Budgets budgets;
    private final UserProfiles userProfiles;
    private final Clock clock;

    HabitsService(
            DayMarkRepository dayMarkRepository,
            UserAchievementRepository achievementRepository,
            Transactions transactions,
            Budgets budgets,
            UserProfiles userProfiles,
            Clock clock) {
        this.dayMarkRepository = dayMarkRepository;
        this.achievementRepository = achievementRepository;
        this.transactions = transactions;
        this.budgets = budgets;
        this.userProfiles = userProfiles;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void markNoSpend(UUID userId, LocalDate date) {
        if (dayMarkRepository.findByUserIdAndDate(userId, date).isPresent()) {
            return;
        }
        dayMarkRepository.save(new DayMark(userId, date));
    }

    @Override
    // Not readOnly: buildDays() -> Budgets.getBudget() may copy-forward + save a budget row
    // the first time the current month is queried, same as any other budget read already does.
    @Transactional
    public HabitSummary getSummary(UUID userId) {
        ZoneId zone = userProfiles.zoneOf(userId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        StreakCalculator.Result result = StreakCalculator.calculate(
                new StreakCalculator.Input(buildDays(userId, zone, today), today, YearMonth.from(today), null));

        List<UserAchievement> achieved = achievementRepository.findAllByUserId(userId);
        List<AchievementView> views = List.of(AchievementCode.values()).stream()
                .map(code -> achieved.stream()
                        .filter(a -> a.getCode() == code)
                        .findFirst()
                        .map(a -> new AchievementView(code, true, a.getAchievedAt()))
                        .orElseGet(() -> new AchievementView(code, false, null)))
                .toList();

        return new HabitSummary(result.currentStreak(), result.bestStreak(), result.daysWithinLimitThisMonth(), views);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<LocalDate> countedDatesBetween(UUID userId, LocalDate from, LocalDate toInclusive) {
        ZoneId zone = userProfiles.zoneOf(userId);
        Set<LocalDate> counted = new java.util.HashSet<>(transactions.activeDates(userId, zone, from, toInclusive));
        for (DayMark mark : dayMarkRepository.findAllByUserIdAndDateBetween(userId, from, toInclusive)) {
            counted.add(mark.getDate());
        }
        return counted;
    }

    @Override
    @Transactional
    public List<AchievementCode> recomputeAndUnlockAchievements(UUID userId) {
        ZoneId zone = userProfiles.zoneOf(userId);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);

        StreakCalculator.Result result = StreakCalculator.calculate(new StreakCalculator.Input(
                buildDays(userId, zone, today), today, YearMonth.from(today), previousMonth));

        List<AchievementCode> newlyUnlocked = new ArrayList<>();
        unlockIfNew(userId, AchievementCode.STREAK_7, result.streak7Achieved(), newlyUnlocked);
        unlockIfNew(userId, AchievementCode.STREAK_30, result.streak30Achieved(), newlyUnlocked);
        unlockIfNew(userId, AchievementCode.LIMIT_WEEK, result.limitWeekAchieved(), newlyUnlocked);
        unlockIfNew(userId, AchievementCode.FULL_MONTH, result.fullMonthAchieved(), newlyUnlocked);
        return newlyUnlocked;
    }

    private void unlockIfNew(UUID userId, AchievementCode code, boolean qualifies, List<AchievementCode> newlyUnlocked) {
        if (!qualifies || achievementRepository.existsByUserIdAndCode(userId, code)) {
            return;
        }
        achievementRepository.save(new UserAchievement(userId, code));
        newlyUnlocked.add(code);
    }

    private List<DayStatus> buildDays(UUID userId, ZoneId zone, LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate previousMonthStart = currentMonth.minusMonths(1).atDay(1);
        LocalDate lookbackStart = today.minusDays(LOOKBACK_DAYS - 1);
        LocalDate from = previousMonthStart.isBefore(lookbackStart) ? previousMonthStart : lookbackStart;

        Set<LocalDate> activeDates = transactions.activeDates(userId, zone, from, today);
        Set<LocalDate> noSpendDates = dayMarkRepository.findAllByUserIdAndDateBetween(userId, from, today).stream()
                .map(DayMark::getDate)
                .collect(Collectors.toSet());

        WithinLimitLookup withinLimit = withinLimitForCurrentMonth(userId, zone, currentMonth);

        List<DayStatus> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            boolean counted = activeDates.contains(d) || noSpendDates.contains(d);
            days.add(new DayStatus(d, counted, withinLimit.isWithinLimit(d)));
        }
        return days;
    }

    /** Reconstructs each past day's safe-to-spend result with {@link BudgetCalculator} -- the
     * same formula the live "today" screen uses, just re-run with an earlier {@code now}. */
    private WithinLimitLookup withinLimitForCurrentMonth(UUID userId, ZoneId zone, YearMonth month) {
        Optional<Budgets.BudgetView> budget = budgets.getBudget(userId, month);
        if (budget.isEmpty()) {
            return date -> false;
        }
        Budgets.BudgetView view = budget.get();
        List<BudgetCalculator.Entry> entries = transactions.monthEntries(userId, zone, month).stream()
                .map(e -> new BudgetCalculator.Entry(e.type(), e.amount(), e.isFixed(), e.transactionDate()))
                .toList();
        BigDecimal fixedPayments = view.fixedPayments() == null ? BigDecimal.ZERO : view.fixedPayments();
        BigDecimal loanPayments = view.loanPayments() == null ? BigDecimal.ZERO : view.loanPayments();
        BigDecimal plannedSavings = view.plannedSavings() == null ? BigDecimal.ZERO : view.plannedSavings();
        BigDecimal expectedIncome = view.expectedIncome() == null ? BigDecimal.ZERO : view.expectedIncome();

        return date -> {
            if (!YearMonth.from(date).equals(month)) {
                return false;
            }
            Instant asOfThatDay = date.atTime(12, 0).atZone(zone).toInstant();
            BudgetCalculator.Result result = BudgetCalculator.calculate(new BudgetCalculator.Input(
                    expectedIncome, fixedPayments, loanPayments, plannedSavings, entries, zone, asOfThatDay));
            return !result.overspentToday();
        };
    }

    private interface WithinLimitLookup {
        boolean isWithinLimit(LocalDate date);
    }

}
