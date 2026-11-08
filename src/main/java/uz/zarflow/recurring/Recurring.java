package uz.zarflow.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * The recurring module's public API. Other modules talk to recurring rules/occurrences
 * only through here — never through {@code RecurringRule}/{@code RecurringOccurrence}
 * or their repositories.
 */
public interface Recurring {

    List<RuleView> listForUser(UUID userId);

    RuleView create(UUID userId, CreateRule input);

    RuleView update(UUID userId, UUID ruleId, UpdateRule input);

    RuleView archive(UUID userId, UUID ruleId);

    /** All due dates between today and today+days, materializing an occurrence row for each
     * if one doesn't exist yet, nearest-first. */
    List<UpcomingOccurrence> upcoming(UUID userId, int days);

    /** Idempotent: a double tap on an already-PAID/SKIPPED occurrence just returns its current state. */
    OccurrenceResult pay(UUID userId, UUID occurrenceId);

    OccurrenceResult skip(UUID userId, UUID occurrenceId);

    /** The budget module's "fixed_payments/loan_payments auto" sums (docs/PRD.md M7 §3). */
    RecurringBudgetAutoSum.Totals autoSumForMonth(UUID userId, YearMonth month);

    record CreateRule(
            String name, RecurringKind kind, BigDecimal amount, UUID categoryId, UUID accountId,
            RecurringDateCalculator.ScheduleKind schedule, Integer dayOfMonth, Integer dayOfWeek, Integer everyNDays,
            LocalDate startDate, boolean autoCreate, int remindDaysBefore) {
    }

    record UpdateRule(
            String name, BigDecimal amount, UUID categoryId, UUID accountId,
            RecurringDateCalculator.ScheduleKind schedule, Integer dayOfMonth, Integer dayOfWeek, Integer everyNDays,
            boolean autoCreate, int remindDaysBefore) {
    }

    record RuleView(
            UUID id, String name, RecurringKind kind, BigDecimal amount, UUID categoryId, UUID accountId,
            RecurringDateCalculator.ScheduleKind schedule, Integer dayOfMonth, Integer dayOfWeek, Integer everyNDays,
            LocalDate startDate, LocalDate nextDueDate, boolean autoCreate, int remindDaysBefore, boolean active) {
    }

    record UpcomingOccurrence(
            UUID occurrenceId, UUID ruleId, String ruleName, RecurringKind kind, BigDecimal amount, UUID accountId,
            LocalDate dueDate, OccurrenceStatus status) {
    }

    record OccurrenceResult(UUID occurrenceId, OccurrenceStatus status, UUID transactionId) {
    }

    /** Published when a reminder should be shown (actionNeeded) or a plain auto-pay
     * confirmation sent (!actionNeeded) — the telegram module listens for this. */
    record ReminderDue(
            UUID userId, UUID occurrenceId, UUID ruleId, String ruleName, RecurringKind kind, BigDecimal amount,
            UUID categoryId, UUID accountId, LocalDate dueDate, boolean dueToday, boolean actionNeeded) {
    }

}
