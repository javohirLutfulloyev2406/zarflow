package uz.zarflow.debt;

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
@Table(name = "debt_schedule")
class DebtScheduleRow {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID debtId;

    private int seq;

    private LocalDate dueDate;

    private BigDecimal principal;

    private BigDecimal interest;

    private BigDecimal fee;

    private boolean isEstimated;

    private UUID paidPaymentId;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected DebtScheduleRow() {
    }

    DebtScheduleRow(UUID debtId, int seq, LocalDate dueDate, BigDecimal principal, BigDecimal interest, BigDecimal fee, boolean isEstimated) {
        this.debtId = debtId;
        this.seq = seq;
        this.dueDate = dueDate;
        this.principal = principal;
        this.interest = interest;
        this.fee = fee;
        this.isEstimated = isEstimated;
    }

    void markPaid(UUID paymentId) {
        this.paidPaymentId = paymentId;
    }

    boolean isPaid() {
        return paidPaymentId != null;
    }

    UUID getId() {
        return id;
    }

    UUID getDebtId() {
        return debtId;
    }

    int getSeq() {
        return seq;
    }

    LocalDate getDueDate() {
        return dueDate;
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

    boolean isEstimated() {
        return isEstimated;
    }

    UUID getPaidPaymentId() {
        return paidPaymentId;
    }

}
