package uz.zarflow.savings;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "savings_goals")
class SavingsGoal {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private String name;

    private BigDecimal targetAmount;

    private LocalDate targetDate;

    private UUID accountId;

    private boolean isEmergency;

    @Enumerated(EnumType.STRING)
    private GoalStatus status;

    private Instant achievedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected SavingsGoal() {
    }

    SavingsGoal(UUID userId, String name, BigDecimal targetAmount, LocalDate targetDate, UUID accountId, boolean isEmergency) {
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.accountId = accountId;
        this.isEmergency = isEmergency;
        this.status = GoalStatus.ACTIVE;
    }

    void update(String name, BigDecimal targetAmount, LocalDate targetDate) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
    }

    void archive() {
        this.status = GoalStatus.ARCHIVED;
    }

    void markAchieved(Instant now) {
        this.status = GoalStatus.ACHIEVED;
        this.achievedAt = now;
    }

    /** An achieved goal goes back to ACTIVE if money is withdrawn below target again. */
    void markActive() {
        this.status = GoalStatus.ACTIVE;
        this.achievedAt = null;
    }

    boolean isActive() {
        return status == GoalStatus.ACTIVE;
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

    BigDecimal getTargetAmount() {
        return targetAmount;
    }

    LocalDate getTargetDate() {
        return targetDate;
    }

    UUID getAccountId() {
        return accountId;
    }

    boolean isEmergency() {
        return isEmergency;
    }

    GoalStatus getStatus() {
        return status;
    }

    Instant getAchievedAt() {
        return achievedAt;
    }

}
