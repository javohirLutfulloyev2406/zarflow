package uz.zarflow.savings;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leftover_offers")
class LeftoverOffer {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    /** First day of the month this offer covers. */
    private LocalDate month;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private LeftoverStatus status;

    private BigDecimal goalAmount;

    private BigDecimal debtAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    protected LeftoverOffer() {
    }

    LeftoverOffer(UUID userId, LocalDate month, BigDecimal amount) {
        this.userId = userId;
        this.month = month;
        this.amount = amount;
        this.status = LeftoverStatus.OFFERED;
    }

    /** Idempotent: a double tap only ever applies the resolution once. */
    boolean resolve(LeftoverStatus resolution, BigDecimal goalAmount, BigDecimal debtAmount, Instant now) {
        if (status != LeftoverStatus.OFFERED) {
            return false;
        }
        this.status = resolution;
        this.goalAmount = goalAmount;
        this.debtAmount = debtAmount;
        this.resolvedAt = now;
        return true;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    LocalDate getMonth() {
        return month;
    }

    BigDecimal getAmount() {
        return amount;
    }

    LeftoverStatus getStatus() {
        return status;
    }

    BigDecimal getGoalAmount() {
        return goalAmount;
    }

    BigDecimal getDebtAmount() {
        return debtAmount;
    }

    Instant getResolvedAt() {
        return resolvedAt;
    }

}
