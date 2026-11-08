package uz.zarflow.budget;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "budget_limits")
class BudgetLimit {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID budgetId;

    private UUID categoryId;

    private BigDecimal amount;

    // Explicit column names: Hibernate's naming strategy only inserts an underscore at a
    // lowercase-to-uppercase transition, so the implicit name for "notified80" would be
    // "notified80", not the migration's "notified_80".
    @Column(name = "notified_80")
    private boolean notified80;

    @Column(name = "notified_100")
    private boolean notified100;

    protected BudgetLimit() {
    }

    BudgetLimit(UUID budgetId, UUID categoryId, BigDecimal amount) {
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.amount = amount;
    }

    void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }

    void markNotified80() {
        this.notified80 = true;
    }

    void markNotified100() {
        this.notified100 = true;
    }

    UUID getId() {
        return id;
    }

    UUID getBudgetId() {
        return budgetId;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    boolean isNotified80() {
        return notified80;
    }

    boolean isNotified100() {
        return notified100;
    }

}
