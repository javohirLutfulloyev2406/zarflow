package uz.zarflow.humo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A pending reconciliation offered via bot buttons: linked (accountId set, a balance
 * mismatch to correct or skip) or unlinked (accountId null, offer to link or ignore the
 * card). Deleted once acted on -- same lifecycle as telegram_pending_entries. */
@Entity
@Table(name = "humo_reconciliation_drafts")
class HumoReconciliationDraft {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private UUID accountId;

    private String cardLast4;

    private BigDecimal bankBalance;

    private Instant asOf;

    @CreationTimestamp
    private Instant createdAt;

    protected HumoReconciliationDraft() {
    }

    HumoReconciliationDraft(UUID userId, UUID accountId, String cardLast4, BigDecimal bankBalance, Instant asOf) {
        this.userId = userId;
        this.accountId = accountId;
        this.cardLast4 = cardLast4;
        this.bankBalance = bankBalance;
        this.asOf = asOf;
    }

    void link(UUID accountId) {
        this.accountId = accountId;
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

    String getCardLast4() {
        return cardLast4;
    }

    BigDecimal getBankBalance() {
        return bankBalance;
    }

    Instant getAsOf() {
        return asOf;
    }

}