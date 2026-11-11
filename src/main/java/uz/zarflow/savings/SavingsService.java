package uz.zarflow.savings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.account.AccountType;
import uz.zarflow.account.Accounts;
import uz.zarflow.budget.Budgets;
import uz.zarflow.debt.DebtStatus;
import uz.zarflow.debt.Debts;
import uz.zarflow.habit.AchievementCode;
import uz.zarflow.habit.Habits;
import uz.zarflow.shared.exception.BusinessRuleException;
import uz.zarflow.shared.security.OwnershipGuard;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@Service
class SavingsService implements Savings {

    /** How many trailing months feed the "pace" average in {@link SavingsGoalCalculator}. */
    private static final int PACE_MONTHS = 3;

    private final SavingsGoalRepository goalRepository;
    private final LeftoverOfferRepository offerRepository;
    private final Transactions transactions;
    private final Accounts accounts;
    private final Budgets budgets;
    private final Debts debts;
    private final Habits habits;
    private final UserProfiles userProfiles;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    SavingsService(
            SavingsGoalRepository goalRepository,
            LeftoverOfferRepository offerRepository,
            Transactions transactions,
            Accounts accounts,
            Budgets budgets,
            Debts debts,
            Habits habits,
            UserProfiles userProfiles,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.goalRepository = goalRepository;
        this.offerRepository = offerRepository;
        this.transactions = transactions;
        this.accounts = accounts;
        this.budgets = budgets;
        this.debts = debts;
        this.habits = habits;
        this.userProfiles = userProfiles;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalView> listForUser(UUID userId) {
        return goalRepository.findAllByUserId(userId).stream().map(g -> toView(userId, g)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalView get(UUID userId, UUID goalId) {
        return toView(userId, loadOwned(userId, goalId));
    }

    @Override
    @Transactional
    public GoalView create(UUID userId, CreateGoal input) {
        requireSavingsAccount(userId, input.accountId());
        if (input.isEmergency() && goalRepository.findByUserIdAndIsEmergencyTrueAndStatus(userId, GoalStatus.ACTIVE).isPresent()) {
            throw new BusinessRuleException("savings.emergency_goal_already_exists");
        }
        SavingsGoal goal = new SavingsGoal(userId, input.name(), input.targetAmount(), input.targetDate(), input.accountId(), input.isEmergency());
        goalRepository.save(goal);
        return toView(userId, goal);
    }

    @Override
    @Transactional
    public GoalView update(UUID userId, UUID goalId, UpdateGoal input) {
        SavingsGoal goal = loadOwned(userId, goalId);
        goal.update(input.name(), input.targetAmount(), input.targetDate());
        goalRepository.save(goal);
        return toView(userId, goal);
    }

    @Override
    @Transactional
    public GoalView archive(UUID userId, UUID goalId) {
        SavingsGoal goal = loadOwned(userId, goalId);
        goal.archive();
        goalRepository.save(goal);
        return toView(userId, goal);
    }

    @Override
    @Transactional
    public ContributionResult contribute(UUID userId, UUID goalId, UUID fromAccountId, BigDecimal amount) {
        SavingsGoal goal = loadOwned(userId, goalId);
        transactions.transferForGoal(userId, new Transactions.GoalTransferInput(
                fromAccountId, goal.getAccountId(), amount, goalId, goal.getName()));
        return afterTransfer(userId, goal);
    }

    @Override
    @Transactional
    public ContributionResult withdraw(UUID userId, UUID goalId, UUID toAccountId, BigDecimal amount) {
        SavingsGoal goal = loadOwned(userId, goalId);
        transactions.transferForGoal(userId, new Transactions.GoalTransferInput(
                goal.getAccountId(), toAccountId, amount, goalId, goal.getName()));
        return afterTransfer(userId, goal);
    }

    private ContributionResult afterTransfer(UUID userId, SavingsGoal goal) {
        BigDecimal saved = transactions.goalSavedAmount(userId, goal.getId(), goal.getAccountId());
        boolean achieved = saved.compareTo(goal.getTargetAmount()) >= 0;

        if (achieved && goal.getStatus() != GoalStatus.ACHIEVED) {
            goal.markAchieved(Instant.now(clock));
            goalRepository.save(goal);
            boolean firstGoal = habits.unlockAchievement(userId, AchievementCode.FIRST_GOAL);
            eventPublisher.publishEvent(new GoalAchieved(userId, goal.getId(), goal.getName(), firstGoal));
        } else if (!achieved && goal.getStatus() == GoalStatus.ACHIEVED) {
            goal.markActive();
            goalRepository.save(goal);
        }
        return new ContributionResult(goal.getId(), saved, achieved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionView> history(UUID userId, UUID goalId) {
        SavingsGoal goal = loadOwned(userId, goalId);
        return transactions.goalHistory(userId, goalId, goal.getAccountId()).stream()
                .map(e -> new ContributionView(e.id(), e.transactionDate(), e.amount()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> suggestedEmergencyTarget(UUID userId) {
        if (goalRepository.findByUserIdAndIsEmergencyTrueAndStatus(userId, GoalStatus.ACTIVE).isPresent()) {
            return Optional.empty();
        }
        ZoneId zone = userProfiles.zoneOf(userId);
        YearMonth month = YearMonth.from(LocalDate.now(clock.withZone(zone)));
        return budgets.getBudget(userId, month).map(b -> {
            BigDecimal fixed = b.fixedPayments() == null ? BigDecimal.ZERO : b.fixedPayments();
            BigDecimal loan = b.loanPayments() == null ? BigDecimal.ZERO : b.loanPayments();
            return fixed.add(loan).setScale(2, RoundingMode.HALF_EVEN);
        });
    }

    @Override
    @Transactional
    public void evaluateLeftover(UUID userId, YearMonth month) {
        if (offerRepository.findByUserIdAndMonth(userId, month.atDay(1)).isPresent()) {
            return;
        }
        ZoneId zone = userProfiles.zoneOf(userId);
        Optional<Budgets.BudgetView> budget = budgets.getBudget(userId, month);
        if (budget.isEmpty()) {
            return;
        }
        List<LeftoverCalculator.Entry> entries = transactions.monthEntries(userId, zone, month).stream()
                .map(e -> new LeftoverCalculator.Entry(e.type(), e.amount(), e.isFixed()))
                .toList();
        LeftoverCalculator.Result result = LeftoverCalculator.calculate(
                new LeftoverCalculator.Input(budget.get().freeBudget(), entries));
        if (!result.positive()) {
            return;
        }
        offerRepository.save(new LeftoverOffer(userId, month.atDay(1), result.leftover()));
        eventPublisher.publishEvent(new LeftoverOffered(userId, month, result.leftover()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LeftoverOfferView> currentOffer(UUID userId) {
        LocalDate now = LocalDate.now(clock.withZone(userProfiles.zoneOf(userId)));
        return offerRepository.findByUserIdAndMonth(userId, YearMonth.from(now).atDay(1))
                .filter(o -> o.getStatus() == LeftoverStatus.OFFERED)
                .or(() -> offerRepository.findByUserIdAndMonth(userId, YearMonth.from(now).minusMonths(1).atDay(1))
                        .filter(o -> o.getStatus() == LeftoverStatus.OFFERED))
                .map(this::toOfferView);
    }

    @Override
    @Transactional
    public LeftoverOfferView resolve(UUID userId, YearMonth month, Resolution resolution) {
        LeftoverOffer offer = offerRepository.findByUserIdAndMonth(userId, month.atDay(1))
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new uz.zarflow.shared.exception.NotFoundException("common.not-found", "leftover_offer"));
        if (offer.getStatus() != LeftoverStatus.OFFERED) {
            return toOfferView(offer);
        }

        BigDecimal amount = offer.getAmount();
        LocalDate today = LocalDate.now(clock.withZone(userProfiles.zoneOf(userId)));
        BigDecimal goalPortion = BigDecimal.ZERO.setScale(2);
        BigDecimal debtPortion = BigDecimal.ZERO.setScale(2);

        switch (resolution.type()) {
            case TO_GOAL -> {
                goalPortion = amount;
                applyToEmergencyGoal(userId, goalPortion);
            }
            case TO_DEBT -> {
                debtPortion = amount;
                applyToDebt(userId, debtPortion, today);
            }
            case SPLIT -> {
                goalPortion = resolution.splitGoalAmount();
                debtPortion = resolution.splitDebtAmount();
                if (goalPortion.signum() > 0) {
                    applyToEmergencyGoal(userId, goalPortion);
                }
                if (debtPortion.signum() > 0) {
                    applyToDebt(userId, debtPortion, today);
                }
            }
            case DISMISSED -> {
                // Nothing to apply.
            }
        }

        LeftoverStatus status = switch (resolution.type()) {
            case TO_GOAL -> LeftoverStatus.TO_GOAL;
            case TO_DEBT -> LeftoverStatus.TO_DEBT;
            case SPLIT -> LeftoverStatus.SPLIT;
            case DISMISSED -> LeftoverStatus.DISMISSED;
        };
        offer.resolve(status, goalPortion, debtPortion, Instant.now(clock));
        offerRepository.save(offer);
        return toOfferView(offer);
    }

    private void applyToEmergencyGoal(UUID userId, BigDecimal amount) {
        SavingsGoal emergency = goalRepository.findByUserIdAndIsEmergencyTrueAndStatus(userId, GoalStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("savings.emergency_goal_required"));
        UUID fromAccountId = defaultSpendingAccountId(userId);
        transactions.transferForGoal(userId, new Transactions.GoalTransferInput(
                fromAccountId, emergency.getAccountId(), amount, emergency.getId(), emergency.getName()));
        afterTransfer(userId, emergency);
    }

    private void applyToDebt(UUID userId, BigDecimal amount, LocalDate today) {
        var active = debts.listForUser(userId).stream().filter(d -> d.status() == DebtStatus.ACTIVE).toList();
        if (active.isEmpty()) {
            throw new BusinessRuleException("savings.no_active_debt");
        }
        UUID accountId = defaultSpendingAccountId(userId);
        debts.pay(userId, active.get(0).id(), new Debts.PayInput(amount, today, accountId, true));
    }

    private UUID defaultSpendingAccountId(UUID userId) {
        return accounts.listForUser(userId).stream()
                .filter(Accounts.AccountView::isDefault)
                .findFirst()
                .or(() -> accounts.listForUser(userId).stream().findFirst())
                .map(Accounts.AccountView::id)
                .orElseThrow(() -> new BusinessRuleException("savings.no_account"));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthSavingsSummary monthSummary(UUID userId, ZoneId zone, YearMonth month) {
        BigDecimal totalSaved = transactions.monthGoalSavingsNet(userId, zone, month);
        List<uz.zarflow.transaction.Transactions.MonthEntry> entries = transactions.monthEntries(userId, zone, month);
        BigDecimal income = entries.stream()
                .filter(e -> e.type() == TransactionType.INCOME)
                .map(uz.zarflow.transaction.Transactions.MonthEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = income.signum() > 0
                ? totalSaved.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;
        return new MonthSavingsSummary(totalSaved.setScale(2, RoundingMode.HALF_EVEN), percent);
    }

    private LeftoverOfferView toOfferView(LeftoverOffer offer) {
        return new LeftoverOfferView(offer.getId(), YearMonth.from(offer.getMonth()), offer.getAmount(), offer.getStatus());
    }

    private void requireSavingsAccount(UUID userId, UUID accountId) {
        boolean isSavings = accounts.listForUser(userId).stream()
                .anyMatch(a -> a.id().equals(accountId) && a.type() == AccountType.SAVINGS);
        if (!isSavings) {
            throw new BusinessRuleException("savings.account_must_be_savings");
        }
    }

    private SavingsGoal loadOwned(UUID userId, UUID goalId) {
        return OwnershipGuard.requireFound(goalRepository.findByIdAndUserId(goalId, userId), "savings_goal");
    }

    private GoalView toView(UUID userId, SavingsGoal goal) {
        BigDecimal saved = transactions.goalSavedAmount(userId, goal.getId(), goal.getAccountId());
        List<BigDecimal> recentMonths = recentMonthlyContributions(userId, goal);
        LocalDate today = LocalDate.now(clock.withZone(userProfiles.zoneOf(userId)));

        SavingsGoalCalculator.Result result = SavingsGoalCalculator.calculate(new SavingsGoalCalculator.Input(
                goal.getTargetAmount(), saved, goal.getTargetDate(), today, recentMonths));

        return new GoalView(
                goal.getId(), goal.getName(), goal.getTargetAmount(), saved, goal.getTargetDate(), goal.getAccountId(),
                goal.isEmergency(), goal.getStatus(), result.remaining(), result.perMonth(), result.perWeek(),
                result.pace(), result.forecastDate(), result.onTrack());
    }

    private List<BigDecimal> recentMonthlyContributions(UUID userId, SavingsGoal goal) {
        ZoneId zone = userProfiles.zoneOf(userId);
        YearMonth current = YearMonth.from(LocalDate.now(clock.withZone(zone)));
        List<BigDecimal> totals = new java.util.ArrayList<>();
        for (int i = PACE_MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal monthTotal = transactions.goalHistory(userId, goal.getId(), goal.getAccountId()).stream()
                    .filter(e -> YearMonth.from(e.transactionDate().atZone(zone)).equals(month))
                    .map(Transactions.GoalTransferEntry::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totals.add(monthTotal);
        }
        return totals;
    }

}
