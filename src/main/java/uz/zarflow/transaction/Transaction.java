package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Instant;
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

import uz.zarflow.category.Necessity;

@Entity
@Table(name = "transactions")
class Transaction {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private UUID accountId;

    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private Necessity necessity;

    private Instant transactionDate;

    private String description;

    @Enumerated(EnumType.STRING)
    private TransactionSource source;

    private UUID transferGroupId;

    private String idempotencyKey;

    private boolean isFixed;

    /** Set only on the two legs of a savings-goal contribution/withdrawal transfer --
     * never on an EXPENSE/INCOME row, where "linked to a goal" has no meaning. */
    private UUID linkedGoalId;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant deletedAt;

    protected Transaction() {
    }

    Transaction(
            UUID userId,
            UUID accountId,
            UUID categoryId,
            TransactionType type,
            BigDecimal amount,
            Necessity necessity,
            Instant transactionDate,
            String description,
            TransactionSource source,
            UUID transferGroupId,
            String idempotencyKey,
            boolean isFixed) {
        this.userId = userId;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.type = type;
        this.amount = amount;
        this.currency = "UZS";
        this.necessity = necessity;
        this.transactionDate = transactionDate;
        this.description = description;
        this.source = source;
        this.transferGroupId = transferGroupId;
        this.idempotencyKey = idempotencyKey;
        this.isFixed = isFixed;
    }

    void update(UUID accountId, UUID categoryId, BigDecimal amount, Necessity necessity,
            Instant transactionDate, String description, boolean isFixed) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.necessity = necessity;
        this.transactionDate = transactionDate;
        this.description = description;
        this.isFixed = isFixed;
    }

    void softDelete(Instant at) {
        this.deletedAt = at;
    }

    void linkGoal(UUID goalId) {
        this.linkedGoalId = goalId;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getAccountId() {
        return accountId;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    TransactionType getType() {
        return type;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getCurrency() {
        return currency;
    }

    Necessity getNecessity() {
        return necessity;
    }

    Instant getTransactionDate() {
        return transactionDate;
    }

    String getDescription() {
        return description;
    }

    TransactionSource getSource() {
        return source;
    }

    UUID getTransferGroupId() {
        return transferGroupId;
    }

    UUID getLinkedGoalId() {
        return linkedGoalId;
    }

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    boolean isFixed() {
        return isFixed;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getDeletedAt() {
        return deletedAt;
    }

}
