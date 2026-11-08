package uz.zarflow.insight;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "insights")
class Insight {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private InsightCode code;

    private String periodKey;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    private int priority;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant sentInBotAt;

    private Instant dismissedAt;

    protected Insight() {
    }

    Insight(UUID userId, InsightCode code, String periodKey, String payload, int priority) {
        this.userId = userId;
        this.code = code;
        this.periodKey = periodKey;
        this.payload = payload;
        this.priority = priority;
    }

    /** Refreshes payload/priority on a later re-evaluation of the same period -- a dismissed
     * insight is never resurrected by this (callers must check {@link #isDismissed} first). */
    void refresh(String payload, int priority) {
        this.payload = payload;
        this.priority = priority;
    }

    void markSentInBot() {
        this.sentInBotAt = Instant.now();
    }

    void dismiss() {
        this.dismissedAt = Instant.now();
    }

    boolean isDismissed() {
        return dismissedAt != null;
    }

    boolean isSentInBot() {
        return sentInBotAt != null;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    InsightCode getCode() {
        return code;
    }

    String getPeriodKey() {
        return periodKey;
    }

    String getPayload() {
        return payload;
    }

    int getPriority() {
        return priority;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

}
