package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/debts")
class DebtController {

    private final Debts debts;
    private final CurrentUser currentUser;

    DebtController(Debts debts, CurrentUser currentUser) {
        this.debts = debts;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<DebtResponse> list() {
        return debts.listForUser(currentUser.requireUserId()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    DebtResponse get(@PathVariable UUID id) {
        return toResponse(debts.get(currentUser.requireUserId(), id));
    }

    @PostMapping
    DebtResponse create(@Valid @RequestBody CreateDebtRequest request) {
        Debts.CreateDebt input = new Debts.CreateDebt(
                request.name(), request.lender(), request.type(), request.originalAmount(), request.annualRate(),
                request.interestMethod(), request.startDate(), request.termMonths(), request.endDate(), request.paymentDay());
        return toResponse(debts.create(currentUser.requireUserId(), input));
    }

    @PutMapping("/{id}")
    DebtResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDebtRequest request) {
        Debts.UpdateDebt input = new Debts.UpdateDebt(
                request.name(), request.lender(), request.annualRate(), request.interestMethod(), request.paymentDay());
        return toResponse(debts.update(currentUser.requireUserId(), id, input));
    }

    @PostMapping("/{id}/manual-schedule")
    DebtResponse setManualSchedule(@PathVariable UUID id, @Valid @RequestBody ManualScheduleRequest request) {
        List<Debts.ManualRow> rows = request.rows().stream()
                .map(r -> new Debts.ManualRow(r.dueDate(), r.principal(), r.interest(), r.fee()))
                .toList();
        return toResponse(debts.setManualSchedule(currentUser.requireUserId(), id, rows));
    }

    @GetMapping("/{id}/schedule")
    List<ScheduleRowResponse> schedule(@PathVariable UUID id) {
        return debts.listSchedule(currentUser.requireUserId(), id).stream()
                .map(r -> new ScheduleRowResponse(r.id(), r.seq(), r.dueDate(), r.principal(), r.interest(), r.fee(), r.isEstimated(), r.paid()))
                .toList();
    }

    @GetMapping("/{id}/payments")
    List<PaymentResponse> payments(@PathVariable UUID id) {
        return debts.listPayments(currentUser.requireUserId(), id).stream()
                .map(p -> new PaymentResponse(p.id(), p.paidOn(), p.principal(), p.interest(), p.fee(), p.extra()))
                .toList();
    }

    @PostMapping("/{id}/payments")
    PaymentResultResponse pay(@PathVariable UUID id, @Valid @RequestBody PayRequest request) {
        Debts.PayInput input = new Debts.PayInput(request.amount(), request.paidOn(), request.accountId(), request.extra());
        Debts.PaymentResult result = debts.pay(currentUser.requireUserId(), id, input);
        return toResultResponse(result);
    }

    @GetMapping("/{id}/progress")
    ProgressResponse progress(@PathVariable UUID id) {
        Debts.ProgressView view = debts.progress(currentUser.requireUserId(), id);
        return new ProgressResponse(view.paidPercent(), view.monthsLeft(), view.estimatedCloseDate(), view.interestRemaining());
    }

    @PostMapping("/{id}/simulate")
    SimulateResponse simulate(@PathVariable UUID id, @Valid @RequestBody SimulateRequest request) {
        Debts.SimulateView view = debts.simulate(currentUser.requireUserId(), id, request.extraMonthly());
        return new SimulateResponse(view.newCloseDate(), view.monthsSaved(), view.interestSaved());
    }

    @PostMapping("/strategy")
    StrategyResponse strategy(@Valid @RequestBody SimulateRequest request) {
        Debts.StrategyView view = debts.strategy(currentUser.requireUserId(), request.extraMonthly());
        return new StrategyResponse(
                new StrategyOutcomeResponse(view.avalanche().closeDate(), view.avalanche().totalInterest()),
                new StrategyOutcomeResponse(view.snowball().closeDate(), view.snowball().totalInterest()),
                view.recommended());
    }

    private DebtResponse toResponse(Debts.DebtView view) {
        return new DebtResponse(
                view.id(), view.name(), view.lender(), view.type(), view.originalAmount(), view.outstandingPrincipal(),
                view.annualRate(), view.monthlyPayment(), view.startDate(), view.endDate(), view.paymentDay(),
                view.interestMethod(), view.status(), view.scheduleIsEstimated());
    }

    private PaymentResultResponse toResultResponse(Debts.PaymentResult result) {
        return new PaymentResultResponse(
                result.debtId(), result.principal(), result.interest(), result.fee(), result.outstandingPrincipal(),
                result.closed(), result.transactionId());
    }

    record CreateDebtRequest(
            @NotBlank String name,
            String lender,
            @NotNull DebtType type,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal originalAmount,
            @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 4) BigDecimal annualRate,
            @NotNull InterestMethod interestMethod,
            @NotNull LocalDate startDate,
            @Min(1) @Max(600) Integer termMonths,
            LocalDate endDate,
            @Min(1) @Max(31) int paymentDay) {
    }

    record UpdateDebtRequest(
            @NotBlank String name,
            String lender,
            @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 4) BigDecimal annualRate,
            @NotNull InterestMethod interestMethod,
            @Min(1) @Max(31) int paymentDay) {
    }

    record ManualRowRequest(
            @NotNull LocalDate dueDate,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal principal,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal interest,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal fee) {
    }

    record ManualScheduleRequest(@NotNull List<@Valid ManualRowRequest> rows) {
    }

    record PayRequest(
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidOn,
            @NotNull UUID accountId,
            boolean extra) {
    }

    record SimulateRequest(
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal extraMonthly) {
    }

    record DebtResponse(
            UUID id, String name, String lender, DebtType type, BigDecimal originalAmount, BigDecimal outstandingPrincipal,
            BigDecimal annualRate, BigDecimal monthlyPayment, LocalDate startDate, LocalDate endDate, int paymentDay,
            InterestMethod interestMethod, DebtStatus status, boolean scheduleIsEstimated) {
    }

    record ScheduleRowResponse(
            UUID id, int seq, LocalDate dueDate, BigDecimal principal, BigDecimal interest, BigDecimal fee,
            boolean isEstimated, boolean paid) {
    }

    record PaymentResponse(UUID id, LocalDate paidOn, BigDecimal principal, BigDecimal interest, BigDecimal fee, boolean extra) {
    }

    record PaymentResultResponse(
            UUID debtId, BigDecimal principal, BigDecimal interest, BigDecimal fee, BigDecimal outstandingPrincipal,
            boolean closed, UUID transactionId) {
    }

    record ProgressResponse(BigDecimal paidPercent, int monthsLeft, LocalDate estimatedCloseDate, BigDecimal interestRemaining) {
    }

    record SimulateResponse(LocalDate newCloseDate, int monthsSaved, BigDecimal interestSaved) {
    }

    record StrategyOutcomeResponse(LocalDate closeDate, BigDecimal totalInterest) {
    }

    record StrategyResponse(
            StrategyOutcomeResponse avalanche, StrategyOutcomeResponse snowball, DebtPayoffStrategy.Strategy recommended) {
    }

}
