package uz.zarflow.recurring;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recurring_rules")
class RecurringRule {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private String name;

    @Enumerated(EnumType.STRING)
    private RecurringKind kind;

    private BigDecimal amount;

    private UUID categoryId;

    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private RecurringDateCalculator.ScheduleKind schedule;

    private Integer dayOfMonth;

    /** ISO-8601: Monday = 1 .. Sunday = 7. */
    private Integer dayOfWeek;

    // Explicit column name: Hibernate's naming strategy doesn't insert an underscore
    // between consecutive capitals, so the implicit name for "everyNDays" would be
    // "everyndays", not the migration's "every_n_days" (see also BudgetLimit.notified80/100).
    @Column(name = "every_n_days")
    private Integer everyNDays;

    private LocalDate startDate;

    private LocalDate nextDueDate;

    private boolean autoCreate;

    private int remindDaysBefore;

    private boolean active;

    private Instant archivedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected RecurringRule() {
    }

    RecurringRule(
            UUID userId, String name, RecurringKind kind, BigDecimal amount, UUID categoryId, UUID accountId,
            RecurringDateCalculator.ScheduleKind schedule, Integer dayOfMonth, Integer dayOfWeek, Integer everyNDays,
            LocalDate startDate, LocalDate nextDueDate, boolean autoCreate, int remindDaysBefore) {
        this.userId = userId;
        this.name = name;
        this.kind = kind;
        this.amount = amount;
        this.categoryId = categoryId;
        this.accountId = accountId;
        this.schedule = schedule;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.everyNDays = everyNDays;
        this.startDate = startDate;
        this.nextDueDate = nextDueDate;
        this.autoCreate = autoCreate;
        this.remindDaysBefore = remindDaysBefore;
        this.active = true;
    }

    void update(
            String name, BigDecimal amount, UUID categoryId, UUID accountId,
            RecurringDateCalculator.ScheduleKind schedule, Integer dayOfMonth, Integer dayOfWeek, Integer everyNDays,
            boolean autoCreate, int remindDaysBefore) {
        this.name = name;
        this.amount = amount;
        this.categoryId = categoryId;
        this.accountId = accountId;
        this.schedule = schedule;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.everyNDays = everyNDays;
        this.autoCreate = autoCreate;
        this.remindDaysBefore = remindDaysBefore;
    }

    void advanceNextDueDate(LocalDate newDate) {
        this.nextDueDate = newDate;
    }

    void archive(Instant at) {
        this.archivedAt = at;
        this.active = false;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getName() {
        return name;
    }

    RecurringKind getKind() {
        return kind;
    }

    BigDecimal getAmount() {
        return amount;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    UUID getAccountId() {
        return accountId;
    }

    RecurringDateCalculator.ScheduleKind getSchedule() {
        return schedule;
    }

    Integer getDayOfMonth() {
        return dayOfMonth;
    }

    Integer getDayOfWeek() {
        return dayOfWeek;
    }

    Integer getEveryNDays() {
        return everyNDays;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getNextDueDate() {
        return nextDueDate;
    }

    boolean isAutoCreate() {
        return autoCreate;
    }

    int getRemindDaysBefore() {
        return remindDaysBefore;
    }

    boolean isActive() {
        return active;
    }

    Instant getArchivedAt() {
        return archivedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

}
