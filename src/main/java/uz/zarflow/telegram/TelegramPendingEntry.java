package uz.zarflow.telegram;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import uz.zarflow.transaction.TransactionType;

/** A quick-input line the parser couldn't categorize, held unsaved until the user taps a category button. */
@Entity
@Table(name = "telegram_pending_entries")
class TelegramPendingEntry {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;

    private String description;

    @CreationTimestamp
    private Instant createdAt;

    protected TelegramPendingEntry() {
    }

    TelegramPendingEntry(UUID userId, UUID accountId, TransactionType type, BigDecimal amount, String description) {
        this.userId = userId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.description = description;
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

    TransactionType getType() {
        return type;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getDescription() {
        return description;
    }

}
