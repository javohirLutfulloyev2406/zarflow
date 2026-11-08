package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * The debt module's public API. Other modules talk to debts only through here — never
 * through {@code Debt}/{@code DebtScheduleRow}/{@code DebtPayment} or their repositories.
 */
public interface Debts {

    List<DebtView> listForUser(UUID userId);

    DebtView get(UUID userId, UUID debtId);

    /** 404s (never 403) when the debt doesn't exist or belongs to someone else -- for other
     * modules (recurring's LOAN-rule-to-debt link) that just need an ownership check. */
    void requireOwned(UUID userId, UUID debtId);

    DebtView create(UUID userId, CreateDebt input);

    DebtView update(UUID userId, UUID debtId, UpdateDebt input);

    /** Replaces the generated (is_estimated=true) schedule with the bank's real rows. */
    DebtView setManualSchedule(UUID userId, UUID debtId, List<ManualRow> rows);

    List<ScheduleRowView> listSchedule(UUID userId, UUID debtId);

    List<PaymentView> listPayments(UUID userId, UUID debtId);

    PaymentResult pay(UUID userId, UUID debtId, PayInput input);

    /** Used by the recurring module's LOAN [To'landi] flow when the rule is linked to a debt
     * (always a regular, non-extra payment; idempotent on {@code idempotencyKey}). */
    PaymentResult payFromRecurring(UUID userId, UUID debtId, BigDecimal amount, LocalDate paidOn, UUID accountId, String idempotencyKey);

    ProgressView progress(UUID userId, UUID debtId);

    SimulateView simulate(UUID userId, UUID debtId, BigDecimal extraMonthly);

    /** Avalanche vs snowball across every ACTIVE debt this user has. */
    StrategyView strategy(UUID userId, BigDecimal extraMonthly);

    /** This local month's interest+fee across all of this user's debt payments -- the
     * analytics module folds this into the "Kredit foizi" category breakdown. */
    BigDecimal monthInterestAndFee(UUID userId, ZoneId zone, YearMonth month);

    /** This local month's principal vs interest+fee split, for the "Kredit: asosiy X, foiz Y" line. */
    MonthDebtTotals monthTotals(UUID userId, ZoneId zone, YearMonth month);

    record CreateDebt(
            String name, String lender, DebtType type, BigDecimal originalAmount, BigDecimal annualRate,
            InterestMethod interestMethod, LocalDate startDate, Integer termMonths, LocalDate endDate, int paymentDay) {
    }

    /** Only metadata and the by-method fallback inputs are editable after creation -- the
     * stored schedule rows (generated or manual) are left exactly as they are. */
    record UpdateDebt(String name, String lender, BigDecimal annualRate, InterestMethod interestMethod, int paymentDay) {
    }

    record ManualRow(LocalDate dueDate, BigDecimal principal, BigDecimal interest, BigDecimal fee) {
    }

    record PayInput(BigDecimal amount, LocalDate paidOn, UUID accountId, boolean extra) {
    }

    record DebtView(
            UUID id, String name, String lender, DebtType type, BigDecimal originalAmount, BigDecimal outstandingPrincipal,
            BigDecimal annualRate, BigDecimal monthlyPayment, LocalDate startDate, LocalDate endDate, int paymentDay,
            InterestMethod interestMethod, DebtStatus status, boolean scheduleIsEstimated) {
    }

    record ScheduleRowView(
            UUID id, int seq, LocalDate dueDate, BigDecimal principal, BigDecimal interest, BigDecimal fee,
            boolean isEstimated, boolean paid) {
    }

    record PaymentView(UUID id, LocalDate paidOn, BigDecimal principal, BigDecimal interest, BigDecimal fee, boolean extra) {
    }

    record PaymentResult(
            UUID debtId, BigDecimal principal, BigDecimal interest, BigDecimal fee, BigDecimal outstandingPrincipal,
            boolean closed, UUID transactionId) {
    }

    record ProgressView(BigDecimal paidPercent, int monthsLeft, LocalDate estimatedCloseDate, BigDecimal interestRemaining) {
    }

    record SimulateView(LocalDate newCloseDate, int monthsSaved, BigDecimal interestSaved) {
    }

    record StrategyOutcome(LocalDate closeDate, BigDecimal totalInterest) {
    }

    record StrategyView(StrategyOutcome avalanche, StrategyOutcome snowball, DebtPayoffStrategy.Strategy recommended) {
    }

    record MonthDebtTotals(BigDecimal principal, BigDecimal interestAndFee) {
    }

    /** Published when a payment brings outstanding principal to zero -- the telegram module
     * listens for this to send a congratulation message. */
    record DebtClosed(UUID userId, UUID debtId, String debtName) {
    }

}
