package uz.zarflow.recurring;

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
@Table(name = "recurring_occurrences")
class RecurringOccurrence {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID ruleId;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private OccurrenceStatus status;

    private UUID transactionId;

    private boolean remindedBeforeSent;

    private boolean remindedDueSent;

    // updatable=false explicit: an occurrence created and then paid/reminded within the same
    // transaction (the auto-create/reminder sweep) otherwise has this column show up (as NULL)
    // in the later UPDATE statement's SET list -- @CreationTimestamp alone wasn't excluding it.
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected RecurringOccurrence() {
    }

    RecurringOccurrence(UUID ruleId, LocalDate dueDate) {
        this.ruleId = ruleId;
        this.dueDate = dueDate;
        this.status = OccurrenceStatus.PENDING;
    }

    void markPaid(UUID transactionId) {
        this.status = OccurrenceStatus.PAID;
        this.transactionId = transactionId;
    }

    void markSkipped() {
        this.status = OccurrenceStatus.SKIPPED;
    }

    void markRemindedBefore() {
        this.remindedBeforeSent = true;
    }

    void markRemindedDue() {
        this.remindedDueSent = true;
    }

    UUID getId() {
        return id;
    }

    UUID getRuleId() {
        return ruleId;
    }

    LocalDate getDueDate() {
        return dueDate;
    }

    OccurrenceStatus getStatus() {
        return status;
    }

    UUID getTransactionId() {
        return transactionId;
    }

    boolean isRemindedBeforeSent() {
        return remindedBeforeSent;
    }

    boolean isRemindedDueSent() {
        return remindedDueSent;
    }

}
