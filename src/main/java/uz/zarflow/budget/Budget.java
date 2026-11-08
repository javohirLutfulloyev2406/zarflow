package uz.zarflow.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "budgets")
class Budget {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private LocalDate month;

    private BigDecimal expectedIncome;

    private BigDecimal fixedPayments;

    private BigDecimal loanPayments;

    private BigDecimal plannedSavings;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Budget() {
    }

    Budget(UUID userId, LocalDate month, BigDecimal expectedIncome, BigDecimal fixedPayments,
            BigDecimal loanPayments, BigDecimal plannedSavings) {
        this.userId = userId;
        this.month = month;
        this.expectedIncome = expectedIncome;
        this.fixedPayments = fixedPayments;
        this.loanPayments = loanPayments;
        this.plannedSavings = plannedSavings;
    }

    void update(BigDecimal expectedIncome, BigDecimal fixedPayments, BigDecimal loanPayments, BigDecimal plannedSavings) {
        this.expectedIncome = expectedIncome;
        this.fixedPayments = fixedPayments;
        this.loanPayments = loanPayments;
        this.plannedSavings = plannedSavings;
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

    BigDecimal getExpectedIncome() {
        return expectedIncome;
    }

    BigDecimal getFixedPayments() {
        return fixedPayments;
    }

    BigDecimal getLoanPayments() {
        return loanPayments;
    }

    BigDecimal getPlannedSavings() {
        return plannedSavings;
    }

}
