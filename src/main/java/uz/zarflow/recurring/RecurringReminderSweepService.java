package uz.zarflow.recurring;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.user.UserProfiles;

/** The per-tick logic behind {@link RecurringReminderScheduler} and the local-only
 * "run as of a given date" dev endpoint. */
@Service
class RecurringReminderSweepService {

    private static final LocalTime REMINDER_TIME = LocalTime.of(10, 0);

    private final RecurringRuleRepository ruleRepository;
    private final RecurringService recurringService;
    private final UserProfiles userProfiles;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    RecurringReminderSweepService(
            RecurringRuleRepository ruleRepository,
            RecurringService recurringService,
            UserProfiles userProfiles,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.ruleRepository = ruleRepository;
        this.recurringService = recurringService;
        this.userProfiles = userProfiles;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** Called every 5 minutes by {@link RecurringScheduler}, guarded by ShedLock against overlap.
     * Public: proxy-based {@code @Transactional} AOP only intercepts public methods. */
    @Transactional
    public void runDueSweep() {
        sweep(null, false);
    }

    /** Local-only: process every rule as if {@code asOfDate} were today, ignoring the 10:00
     * time-of-day gate, so a single call can be tested without waiting for real time. */
    @Transactional
    public void runForDate(LocalDate asOfDate) {
        sweep(asOfDate, true);
    }

    private void sweep(LocalDate overrideToday, boolean ignoreTimeGate) {
        for (RecurringRule rule : ruleRepository.findAllByActiveTrueAndArchivedAtIsNull()) {
            processRule(rule, overrideToday, ignoreTimeGate);
        }
    }

    private void processRule(RecurringRule rule, LocalDate overrideToday, boolean ignoreTimeGate) {
        ZoneId zone = userProfiles.zoneOf(rule.getUserId());
        LocalDate today = overrideToday != null ? overrideToday : LocalDate.now(clock.withZone(zone));
        LocalTime nowLocal = LocalTime.now(clock.withZone(zone));
        LocalDate dueDate = rule.getNextDueDate();
        LocalDate beforeDate = dueDate.minusDays(rule.getRemindDaysBefore());

        if (today.isBefore(beforeDate) || today.isAfter(dueDate)) {
            return;
        }

        RecurringOccurrence occurrence = recurringService.findOrCreateOccurrence(rule.getId(), dueDate);
        if (occurrence.getStatus() != OccurrenceStatus.PENDING) {
            return;
        }

        if (rule.isAutoCreate() && today.equals(dueDate)) {
            autoPay(rule, occurrence);
            return;
        }

        if (!ignoreTimeGate && nowLocal.isBefore(REMINDER_TIME)) {
            return;
        }

        // No explicit save() here: `occurrence` is already the managed instance that
        // findOrCreateOccurrence just inserted in this same transaction, so re-saving it would
        // route through merge() (non-null id => "not new") instead of staying on the insert it
        // just did -- which empirically nulls out @CreationTimestamp on some Hibernate versions.
        // Plain dirty-checking flushes this mutation at commit without needing save() at all.
        if (today.equals(beforeDate) && !occurrence.isRemindedBeforeSent()) {
            occurrence.markRemindedBefore();
            publish(rule, occurrence, today.equals(dueDate), true);
        } else if (today.equals(dueDate) && !occurrence.isRemindedDueSent()) {
            occurrence.markRemindedDue();
            publish(rule, occurrence, true, true);
        }
    }

    private void autoPay(RecurringRule rule, RecurringOccurrence occurrence) {
        UUID transactionId = recurringService.createSettlementTransaction(rule, occurrence);
        occurrence.markPaid(transactionId);
        recurringService.advanceRule(rule, occurrence.getDueDate());
        publish(rule, occurrence, true, false);
    }

    private void publish(RecurringRule rule, RecurringOccurrence occurrence, boolean dueToday, boolean actionNeeded) {
        UUID categoryId = rule.getKind() == RecurringKind.LOAN ? null : rule.getCategoryId();
        eventPublisher.publishEvent(new Recurring.ReminderDue(
                rule.getUserId(), occurrence.getId(), rule.getId(), rule.getName(), rule.getKind(), rule.getAmount(),
                categoryId, rule.getAccountId(), occurrence.getDueDate(), dueToday, actionNeeded));
    }

}
