package uz.zarflow.humo;

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

/** A pending question for one parsed HUMO transaction block, offered via bot buttons
 * (docs/PRD.md §1-5). Advances through {@link State} as each question is answered;
 * deleted once fully resolved -- same lifecycle as telegram_pending_entries /
 * humo_reconciliation_drafts. */
@Entity
@Table(name = "humo_import_drafts")
class HumoImportDraft {

    enum State {
        PENDING_CARD_LINK,
        PENDING_P2P_CHOICE,
        PENDING_P2P_ACCOUNT,
        PENDING_LOAN_PICK,
        PENDING_CATEGORY,
        PENDING_DUPLICATE_CONFIRM
    }

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private UUID bankImportId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;

    private String merchantRaw;

    private String merchantKey;

    private String cardLast4;

    private UUID accountId;

    private UUID categoryId;

    private Instant transactionDate;

    @Enumerated(EnumType.STRING)
    private State state;

    @CreationTimestamp
    private Instant createdAt;

    protected HumoImportDraft() {
    }

    HumoImportDraft(
            UUID userId, UUID bankImportId, TransactionType type, BigDecimal amount, String merchantRaw,
            String merchantKey, String cardLast4, UUID accountId, Instant transactionDate, State state) {
        this.userId = userId;
        this.bankImportId = bankImportId;
        this.type = type;
        this.amount = amount;
        this.merchantRaw = merchantRaw;
        this.merchantKey = merchantKey;
        this.cardLast4 = cardLast4;
        this.accountId = accountId;
        this.transactionDate = transactionDate;
        this.state = state;
    }

    void linkAccount(UUID accountId, State nextState) {
        this.accountId = accountId;
        this.state = nextState;
    }

    void advance(State nextState) {
        this.state = nextState;
    }

    void resolveCategory(UUID categoryId, State nextState) {
        this.categoryId = categoryId;
        this.state = nextState;
    }

    /** P2P classification (docs/PRD.md §4) can override the bank-reported sign once the
     * user explicitly picks [Xarajat]/[Daromad] for a P2P merchant. */
    void retype(TransactionType type) {
        this.type = type;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getBankImportId() {
        return bankImportId;
    }

    TransactionType getType() {
        return type;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getMerchantRaw() {
        return merchantRaw;
    }

    String getMerchantKey() {
        return merchantKey;
    }

    String getCardLast4() {
        return cardLast4;
    }

    UUID getAccountId() {
        return accountId;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    Instant getTransactionDate() {
        return transactionDate;
    }

    State getState() {
        return state;
    }

}
