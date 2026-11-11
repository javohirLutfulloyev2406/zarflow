package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "debt_payments")
class DebtPayment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID debtId;

    private LocalDate paidOn;

    private BigDecimal principal;

    private BigDecimal interest;

    private BigDecimal fee;

    private boolean extra;

    private UUID transactionId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    protected DebtPayment() {
    }

    DebtPayment(UUID debtId, LocalDate paidOn, BigDecimal principal, BigDecimal interest, BigDecimal fee, boolean extra, UUID transactionId) {
        this.debtId = debtId;
        this.paidOn = paidOn;
        this.principal = principal;
        this.interest = interest;
        this.fee = fee;
        this.extra = extra;
        this.transactionId = transactionId;
    }

    UUID getId() {
        return id;
    }

    UUID getDebtId() {
        return debtId;
    }

    LocalDate getPaidOn() {
        return paidOn;
    }

    BigDecimal getPrincipal() {
        return principal;
    }

    BigDecimal getInterest() {
        return interest;
    }

    BigDecimal getFee() {
        return fee;
    }

    boolean isExtra() {
        return extra;
    }

    UUID getTransactionId() {
        return transactionId;
    }

}
