package uz.zarflow.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/recurring")
class RecurringController {

    private final Recurring recurring;
    private final CurrentUser currentUser;

    RecurringController(Recurring recurring, CurrentUser currentUser) {
        this.recurring = recurring;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<RuleResponse> list() {
        return recurring.listForUser(currentUser.requireUserId()).stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RuleResponse create(@Valid @RequestBody CreateRuleRequest request) {
        var input = new Recurring.CreateRule(
                request.name(), request.kind(), request.amount(), request.categoryId(), request.accountId(), request.debtId(),
                request.schedule(), request.dayOfMonth(), request.dayOfWeek(), request.everyNDays(),
                request.startDate(), request.autoCreate(), request.remindDaysBefore());
        return toResponse(recurring.create(currentUser.requireUserId(), input));
    }

    @PutMapping("/{id}")
    RuleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRuleRequest request) {
        var input = new Recurring.UpdateRule(
                request.name(), request.amount(), request.categoryId(), request.accountId(),
                request.schedule(), request.dayOfMonth(), request.dayOfWeek(), request.everyNDays(),
                request.autoCreate(), request.remindDaysBefore());
        return toResponse(recurring.update(currentUser.requireUserId(), id, input));
    }

    @DeleteMapping("/{id}")
    RuleResponse archive(@PathVariable UUID id) {
        return toResponse(recurring.archive(currentUser.requireUserId(), id));
    }

    @GetMapping("/upcoming")
    List<UpcomingResponse> upcoming(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return recurring.upcoming(currentUser.requireUserId(), days).stream().map(this::toResponse).toList();
    }

    @PostMapping("/occurrences/{id}/pay")
    OccurrenceResponse pay(@PathVariable UUID id) {
        return toResponse(recurring.pay(currentUser.requireUserId(), id));
    }

    @PostMapping("/occurrences/{id}/skip")
    OccurrenceResponse skip(@PathVariable UUID id) {
        return toResponse(recurring.skip(currentUser.requireUserId(), id));
    }

    private RuleResponse toResponse(Recurring.RuleView view) {
        return new RuleResponse(
                view.id(), view.name(), view.kind(), view.amount(), view.categoryId(), view.accountId(), view.debtId(),
                view.schedule(), view.dayOfMonth(), view.dayOfWeek(), view.everyNDays(), view.startDate(),
                view.nextDueDate(), view.autoCreate(), view.remindDaysBefore(), view.active());
    }

    private UpcomingResponse toResponse(Recurring.UpcomingOccurrence occurrence) {
        return new UpcomingResponse(
                occurrence.occurrenceId(), occurrence.ruleId(), occurrence.ruleName(), occurrence.kind(),
                occurrence.amount(), occurrence.accountId(), occurrence.dueDate(), occurrence.status());
    }

    private OccurrenceResponse toResponse(Recurring.OccurrenceResult result) {
        return new OccurrenceResponse(result.occurrenceId(), result.status(), result.transactionId());
    }

    record CreateRuleRequest(
            @NotNull @Size(max = 60) String name,
            @NotNull RecurringKind kind,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            UUID categoryId,
            @NotNull UUID accountId,
            UUID debtId,
            @NotNull RecurringDateCalculator.ScheduleKind schedule,
            @Min(1) @Max(31) Integer dayOfMonth,
            @Min(1) @Max(7) Integer dayOfWeek,
            @Min(1) Integer everyNDays,
            @NotNull LocalDate startDate,
            boolean autoCreate,
            @Min(0) @Max(14) int remindDaysBefore) {
    }

    record UpdateRuleRequest(
            @NotNull @Size(max = 60) String name,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            UUID categoryId,
            @NotNull UUID accountId,
            @NotNull RecurringDateCalculator.ScheduleKind schedule,
            @Min(1) @Max(31) Integer dayOfMonth,
            @Min(1) @Max(7) Integer dayOfWeek,
            @Min(1) Integer everyNDays,
            boolean autoCreate,
            @Min(0) @Max(14) int remindDaysBefore) {
    }

    record RuleResponse(
            UUID id, String name, RecurringKind kind, BigDecimal amount, UUID categoryId, UUID accountId, UUID debtId,
            RecurringDateCalculator.ScheduleKind schedule, Integer dayOfMonth, Integer dayOfWeek, Integer everyNDays,
            LocalDate startDate, LocalDate nextDueDate, boolean autoCreate, int remindDaysBefore, boolean active) {
    }

    record UpcomingResponse(
            UUID occurrenceId, UUID ruleId, String ruleName, RecurringKind kind, BigDecimal amount, UUID accountId,
            LocalDate dueDate, OccurrenceStatus status) {
    }

    record OccurrenceResponse(UUID occurrenceId, OccurrenceStatus status, UUID transactionId) {
    }

}
