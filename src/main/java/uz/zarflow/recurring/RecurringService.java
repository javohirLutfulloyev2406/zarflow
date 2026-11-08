package uz.zarflow.recurring;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.account.Accounts;
import uz.zarflow.category.Categories;
import uz.zarflow.shared.exception.NotFoundException;
import uz.zarflow.shared.security.OwnershipGuard;
import uz.zarflow.transaction.TransactionType;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@Service
class RecurringService implements Recurring {

    private final RecurringRuleRepository ruleRepository;
    private final RecurringOccurrenceRepository occurrenceRepository;
    private final Transactions transactions;
    private final Accounts accounts;
    private final Categories categories;
    private final UserProfiles userProfiles;
    private final Clock clock;

    RecurringService(
            RecurringRuleRepository ruleRepository,
            RecurringOccurrenceRepository occurrenceRepository,
            Transactions transactions,
            Accounts accounts,
            Categories categories,
            UserProfiles userProfiles,
            Clock clock) {
        this.ruleRepository = ruleRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.transactions = transactions;
        this.accounts = accounts;
        this.categories = categories;
        this.userProfiles = userProfiles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleView> listForUser(UUID userId) {
        return ruleRepository.findAllByUserIdAndArchivedAtIsNull(userId).stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    public RuleView create(UUID userId, CreateRule input) {
        accounts.requireOwned(userId, input.accountId());
        if (input.kind() != RecurringKind.LOAN) {
            categories.requireUsable(userId, input.categoryId());
        }
        LocalDate today = today(userId);
        RecurringDateCalculator.Schedule schedule = toSchedule(
                input.schedule(), input.dayOfMonth(), input.dayOfWeek(), input.everyNDays(), input.startDate());
        LocalDate nextDueDate = RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, today);

        RecurringRule rule = new RecurringRule(
                userId, input.name(), input.kind(), input.amount(), input.kind() == RecurringKind.LOAN ? null : input.categoryId(),
                input.accountId(), input.schedule(), input.dayOfMonth(), input.dayOfWeek(), input.everyNDays(),
                input.startDate(), nextDueDate, input.autoCreate(), input.remindDaysBefore());
        ruleRepository.save(rule);
        return toView(rule);
    }

    @Override
    @Transactional
    public RuleView update(UUID userId, UUID ruleId, UpdateRule input) {
        RecurringRule rule = requireOwned(userId, ruleId);
        accounts.requireOwned(userId, input.accountId());
        if (rule.getKind() != RecurringKind.LOAN) {
            categories.requireUsable(userId, input.categoryId());
        }
        rule.update(
                input.name(), input.amount(), rule.getKind() == RecurringKind.LOAN ? null : input.categoryId(), input.accountId(),
                input.schedule(), input.dayOfMonth(), input.dayOfWeek(), input.everyNDays(),
                input.autoCreate(), input.remindDaysBefore());

        LocalDate today = today(userId);
        RecurringDateCalculator.Schedule schedule = toSchedule(
                input.schedule(), input.dayOfMonth(), input.dayOfWeek(), input.everyNDays(), rule.getStartDate());
        rule.advanceNextDueDate(RecurringDateCalculator.firstOccurrenceOnOrAfter(schedule, today));
        ruleRepository.save(rule);
        return toView(rule);
    }

    @Override
    @Transactional
    public RuleView archive(UUID userId, UUID ruleId) {
        RecurringRule rule = requireOwned(userId, ruleId);
        rule.archive(java.time.Instant.now(clock));
        ruleRepository.save(rule);
        return toView(rule);
    }

    @Override
    @Transactional
    public List<UpcomingOccurrence> upcoming(UUID userId, int days) {
        LocalDate today = today(userId);
        LocalDate horizon = today.plusDays(days);
        List<RecurringRule> rules = ruleRepository.findAllByUserIdAndArchivedAtIsNull(userId).stream()
                .filter(RecurringRule::isActive)
                .toList();

        List<UpcomingOccurrence> result = new ArrayList<>();
        for (RecurringRule rule : rules) {
            RecurringDateCalculator.Schedule schedule = toSchedule(rule);
            LocalDate dueDate = rule.getNextDueDate();
            for (int guard = 0; guard < 60 && !dueDate.isAfter(horizon); guard++) {
                RecurringOccurrence occurrence = findOrCreateOccurrence(rule.getId(), dueDate);
                result.add(new UpcomingOccurrence(
                        occurrence.getId(), rule.getId(), rule.getName(), rule.getKind(), rule.getAmount(),
                        rule.getAccountId(), dueDate, occurrence.getStatus()));
                dueDate = RecurringDateCalculator.nextAfter(schedule, dueDate);
            }
        }
        return result.stream().sorted((a, b) -> a.dueDate().compareTo(b.dueDate())).toList();
    }

    @Override
    @Transactional
    public OccurrenceResult pay(UUID userId, UUID occurrenceId) {
        RecurringOccurrence occurrence = requireOwnedOccurrence(userId, occurrenceId);
        if (occurrence.getStatus() != OccurrenceStatus.PENDING) {
            return toResult(occurrence);
        }
        RecurringRule rule = ruleRepository.findByIdAndUserId(occurrence.getRuleId(), userId).orElseThrow(
                () -> new NotFoundException("common.not-found", "recurring_rule"));

        UUID transactionId = createSettlementTransaction(rule, occurrence);
        occurrence.markPaid(transactionId);
        occurrenceRepository.save(occurrence);
        advanceRule(rule, occurrence.getDueDate());
        return toResult(occurrence);
    }

    @Override
    @Transactional
    public OccurrenceResult skip(UUID userId, UUID occurrenceId) {
        RecurringOccurrence occurrence = requireOwnedOccurrence(userId, occurrenceId);
        if (occurrence.getStatus() != OccurrenceStatus.PENDING) {
            return toResult(occurrence);
        }
        RecurringRule rule = ruleRepository.findByIdAndUserId(occurrence.getRuleId(), userId).orElseThrow(
                () -> new NotFoundException("common.not-found", "recurring_rule"));

        occurrence.markSkipped();
        occurrenceRepository.save(occurrence);
        advanceRule(rule, occurrence.getDueDate());
        return toResult(occurrence);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringBudgetAutoSum.Totals autoSumForMonth(UUID userId, YearMonth month) {
        List<RecurringBudgetAutoSum.RuleSnapshot> snapshots = ruleRepository.findAllByUserIdAndArchivedAtIsNull(userId).stream()
                .map(rule -> new RecurringBudgetAutoSum.RuleSnapshot(
                        rule.getKind(), rule.getAmount(), rule.isActive(), rule.getArchivedAt(), toSchedule(rule), rule.getStartDate()))
                .toList();
        return RecurringBudgetAutoSum.autoSum(snapshots, month);
    }

    UUID createSettlementTransaction(RecurringRule rule, RecurringOccurrence occurrence) {
        TransactionType type = switch (rule.getKind()) {
            case REGULAR -> TransactionType.EXPENSE;
            case LOAN -> TransactionType.DEBT_PAYMENT;
            case INCOME -> TransactionType.INCOME;
        };
        UUID categoryId = rule.getKind() == RecurringKind.LOAN ? null : rule.getCategoryId();
        boolean isFixed = rule.getKind() == RecurringKind.REGULAR;
        String idempotencyKey = "recurring:" + occurrence.getId();
        Transactions.BotTransaction created = transactions.createFromRecurring(rule.getUserId(), new Transactions.RecurringCreateInput(
                rule.getAccountId(), categoryId, type, rule.getAmount(), isFixed, rule.getName(), idempotencyKey));
        return created.id();
    }

    void advanceRule(RecurringRule rule, LocalDate fromDueDate) {
        rule.advanceNextDueDate(RecurringDateCalculator.nextAfter(toSchedule(rule), fromDueDate));
        ruleRepository.save(rule);
    }

    RecurringOccurrence findOrCreateOccurrence(UUID ruleId, LocalDate dueDate) {
        return occurrenceRepository.findByRuleIdAndDueDate(ruleId, dueDate)
                .orElseGet(() -> occurrenceRepository.save(new RecurringOccurrence(ruleId, dueDate)));
    }

    private RecurringOccurrence requireOwnedOccurrence(UUID userId, UUID occurrenceId) {
        RecurringOccurrence occurrence = occurrenceRepository.findById(occurrenceId).orElseThrow(
                () -> new NotFoundException("common.not-found", "recurring_occurrence"));
        // Always re-check the rule belongs to the user who pressed -- never trust the occurrence id alone.
        ruleRepository.findByIdAndUserId(occurrence.getRuleId(), userId).orElseThrow(
                () -> new NotFoundException("common.not-found", "recurring_occurrence"));
        return occurrence;
    }

    private RecurringRule requireOwned(UUID userId, UUID ruleId) {
        return OwnershipGuard.requireFound(ruleRepository.findByIdAndUserId(ruleId, userId), "recurring_rule");
    }

    private LocalDate today(UUID userId) {
        ZoneId zone = userProfiles.zoneOf(userId);
        return LocalDate.now(clock.withZone(zone));
    }

    private RecurringDateCalculator.Schedule toSchedule(RecurringRule rule) {
        return toSchedule(rule.getSchedule(), rule.getDayOfMonth(), rule.getDayOfWeek(), rule.getEveryNDays(), rule.getStartDate());
    }

    private RecurringDateCalculator.Schedule toSchedule(
            RecurringDateCalculator.ScheduleKind kind, Integer dayOfMonth, Integer dayOfWeek, Integer everyNDays, LocalDate anchor) {
        return switch (kind) {
            case MONTHLY -> RecurringDateCalculator.Schedule.monthly(dayOfMonth);
            case WEEKLY -> RecurringDateCalculator.Schedule.weekly(DayOfWeek.of(dayOfWeek));
            case EVERY_N_DAYS -> RecurringDateCalculator.Schedule.everyNDays(everyNDays, anchor);
        };
    }

    private RuleView toView(RecurringRule rule) {
        return new RuleView(
                rule.getId(), rule.getName(), rule.getKind(), rule.getAmount(), rule.getCategoryId(), rule.getAccountId(),
                rule.getSchedule(), rule.getDayOfMonth(), rule.getDayOfWeek(), rule.getEveryNDays(), rule.getStartDate(),
                rule.getNextDueDate(), rule.isAutoCreate(), rule.getRemindDaysBefore(), rule.isActive());
    }

    private OccurrenceResult toResult(RecurringOccurrence occurrence) {
        return new OccurrenceResult(occurrence.getId(), occurrence.getStatus(), occurrence.getTransactionId());
    }

}
