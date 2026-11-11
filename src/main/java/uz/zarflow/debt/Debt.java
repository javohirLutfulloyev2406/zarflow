package uz.zarflow.debt;

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
@Table(name = "debts")
class Debt {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;

    private String name;

    private String lender;

    @Enumerated(EnumType.STRING)
    private DebtType type;

    private BigDecimal originalAmount;

    private BigDecimal outstandingPrincipal;

    private BigDecimal annualRate;

    private BigDecimal monthlyPayment;

    private LocalDate startDate;

    private LocalDate endDate;

    private int paymentDay;

    @Enumerated(EnumType.STRING)
    private InterestMethod interestMethod;

    @Enumerated(EnumType.STRING)
    private DebtStatus status;

    private Instant closedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Debt() {
    }

    Debt(
            UUID userId, String name, String lender, DebtType type, BigDecimal originalAmount,
            BigDecimal annualRate, BigDecimal monthlyPayment, LocalDate startDate, LocalDate endDate,
            int paymentDay, InterestMethod interestMethod) {
        this.userId = userId;
        this.name = name;
        this.lender = lender;
        this.type = type;
        this.originalAmount = originalAmount;
        this.outstandingPrincipal = originalAmount;
        this.annualRate = annualRate;
        this.monthlyPayment = monthlyPayment;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentDay = paymentDay;
        this.interestMethod = interestMethod;
        this.status = DebtStatus.ACTIVE;
    }

    void update(
            String name, String lender, BigDecimal annualRate, BigDecimal monthlyPayment, LocalDate endDate,
            int paymentDay, InterestMethod interestMethod) {
        this.name = name;
        this.lender = lender;
        this.annualRate = annualRate;
        this.monthlyPayment = monthlyPayment;
        this.endDate = endDate;
        this.paymentDay = paymentDay;
        this.interestMethod = interestMethod;
    }

    /** Returns true if this payment just closed the debt. */
    boolean applyPrincipalPayment(BigDecimal principal, Instant now) {
        this.outstandingPrincipal = outstandingPrincipal.subtract(principal);
        if (outstandingPrincipal.signum() <= 0) {
            this.outstandingPrincipal = BigDecimal.ZERO.setScale(2);
            this.status = DebtStatus.CLOSED;
            this.closedAt = now;
            return true;
        }
        return false;
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

    String getLender() {
        return lender;
    }

    DebtType getType() {
        return type;
    }

    BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    BigDecimal getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    BigDecimal getAnnualRate() {
        return annualRate;
    }

    BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    LocalDate getEndDate() {
        return endDate;
    }

    int getPaymentDay() {
        return paymentDay;
    }

    InterestMethod getInterestMethod() {
        return interestMethod;
    }

    DebtStatus getStatus() {
        return status;
    }

    boolean isActive() {
        return status == DebtStatus.ACTIVE;
    }

    Instant getClosedAt() {
        return closedAt;
    }

}
