package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.shared.exception.BusinessRuleException;
import uz.zarflow.shared.security.OwnershipGuard;
import uz.zarflow.transaction.Transactions;
import uz.zarflow.user.UserProfiles;

@Service
class DebtsService implements Debts {

    private final DebtRepository debtRepository;
    private final DebtScheduleRepository scheduleRepository;
    private final DebtPaymentRepository paymentRepository;
    private final Transactions transactions;
    private final UserProfiles userProfiles;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    DebtsService(
            DebtRepository debtRepository,
            DebtScheduleRepository scheduleRepository,
            DebtPaymentRepository paymentRepository,
            Transactions transactions,
            UserProfiles userProfiles,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.debtRepository = debtRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentRepository = paymentRepository;
        this.transactions = transactions;
        this.userProfiles = userProfiles;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DebtView> listForUser(UUID userId) {
        return debtRepository.findAllByUserId(userId).stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DebtView get(UUID userId, UUID debtId) {
        return toView(loadOwned(userId, debtId));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOwned(UUID userId, UUID debtId) {
        OwnershipGuard.requireFound(debtRepository.findByIdAndUserId(debtId, userId), "debt");
    }

    @Override
    @Transactional
    public DebtView create(UUID userId, CreateDebt input) {
        int termMonths = input.termMonths() != null ? input.termMonths() : monthsBetween(input.startDate(), input.endDate());

        DebtScheduleGenerator.Result generated = DebtScheduleGenerator.generate(new DebtScheduleGenerator.Input(
                input.originalAmount(), input.annualRate(), input.interestMethod(), termMonths, input.startDate(), input.paymentDay()));
        LocalDate endDate = generated.rows().get(generated.rows().size() - 1).dueDate();

        Debt debt = new Debt(
                userId, input.name(), input.lender(), input.type(), input.originalAmount(), input.annualRate(),
                generated.monthlyPayment(), input.startDate(), endDate, input.paymentDay(), input.interestMethod());
        debtRepository.save(debt);
        saveGeneratedSchedule(debt.getId(), generated.rows());
        return toView(debt);
    }

    @Override
    @Transactional
    public DebtView update(UUID userId, UUID debtId, UpdateDebt input) {
        Debt debt = loadOwned(userId, debtId);
        debt.update(input.name(), input.lender(), input.annualRate(), debt.getMonthlyPayment(), debt.getEndDate(),
                input.paymentDay(), input.interestMethod());
        debtRepository.save(debt);
        return toView(debt);
    }

    @Override
    @Transactional
    public DebtView setManualSchedule(UUID userId, UUID debtId, List<ManualRow> rows) {
        Debt debt = loadOwned(userId, debtId);
        scheduleRepository.deleteByDebtId(debtId);

        List<ManualRow> sorted = rows.stream().sorted((a, b) -> a.dueDate().compareTo(b.dueDate())).toList();
        List<DebtScheduleRow> entities = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ManualRow row = sorted.get(i);
            entities.add(new DebtScheduleRow(debtId, i + 1, row.dueDate(), row.principal(), row.interest(), row.fee(), false));
        }
        scheduleRepository.saveAll(entities);

        if (!entities.isEmpty()) {
            debt.update(debt.getName(), debt.getLender(), debt.getAnnualRate(), debt.getMonthlyPayment(),
                    entities.get(entities.size() - 1).getDueDate(), debt.getPaymentDay(), debt.getInterestMethod());
            debtRepository.save(debt);
        }
        return toView(debt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleRowView> listSchedule(UUID userId, UUID debtId) {
        loadOwned(userId, debtId);
        return scheduleRepository.findAllByDebtIdOrderBySeqAsc(debtId).stream().map(this::toRowView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentView> listPayments(UUID userId, UUID debtId) {
        loadOwned(userId, debtId);
        return paymentRepository.findAllByDebtIdOrderByPaidOnDesc(debtId).stream()
                .map(p -> new PaymentView(p.getId(), p.getPaidOn(), p.getPrincipal(), p.getInterest(), p.getFee(), p.isExtra()))
                .toList();
    }

    @Override
    @Transactional
    public PaymentResult pay(UUID userId, UUID debtId, PayInput input) {
        Debt debt = loadOwned(userId, debtId);
        String idempotencyKey = "debt:" + debtId + ":" + input.paidOn() + ":" + input.amount() + ":" + input.extra();
        return payInternal(debt, input.amount(), input.paidOn(), input.accountId(), input.extra(), idempotencyKey);
    }

    @Override
    @Transactional
    public PaymentResult payFromRecurring(
            UUID userId, UUID debtId, BigDecimal amount, LocalDate paidOn, UUID accountId, String idempotencyKey) {
        Debt debt = loadOwned(userId, debtId);
        return payInternal(debt, amount, paidOn, accountId, false, idempotencyKey);
    }

    private PaymentResult payInternal(
            Debt debt, BigDecimal amount, LocalDate paidOn, UUID accountId, boolean extra, String idempotencyKey) {
        if (!debt.isActive()) {
            throw new BusinessRuleException("debt.already_closed");
        }

        DebtScheduleRow matchingRow = scheduleRepository
                .findFirstByDebtIdAndPaidPaymentIdIsNullOrderByDueDateAsc(debt.getId()).orElse(null);
        long daysInPeriod = ChronoUnit.DAYS.between(lastPaymentOrStart(debt), paidOn);
        if (daysInPeriod <= 0) {
            daysInPeriod = 1;
        }

        PaymentSplitter.ScheduleRow rowForSplit = matchingRow == null ? null
                : new PaymentSplitter.ScheduleRow(matchingRow.getPrincipal(), matchingRow.getInterest(), matchingRow.getFee());
        PaymentSplitter.Split split = PaymentSplitter.split(new PaymentSplitter.Input(
                amount, extra, debt.getOutstandingPrincipal(), rowForSplit, debt.getAnnualRate(), debt.getInterestMethod(), daysInPeriod));

        BigDecimal transactionAmount = split.principal().add(split.interest()).add(split.fee());
        var transaction = transactions.createFromDebtPayment(debt.getUserId(), new Transactions.DebtPaymentCreateInput(
                accountId, transactionAmount, debt.getName(), idempotencyKey));

        DebtPayment payment = new DebtPayment(
                debt.getId(), paidOn, split.principal(), split.interest(), split.fee(), extra, transaction.id());
        paymentRepository.save(payment);
        if (matchingRow != null) {
            matchingRow.markPaid(payment.getId());
            scheduleRepository.save(matchingRow);
        }

        boolean closed = debt.applyPrincipalPayment(split.principal(), Instant.now(clock));
        debtRepository.save(debt);
        if (closed) {
            eventPublisher.publishEvent(new DebtClosed(debt.getUserId(), debt.getId(), debt.getName()));
        }

        return new PaymentResult(debt.getId(), split.principal(), split.interest(), split.fee(),
                debt.getOutstandingPrincipal(), closed, transaction.id());
    }

    private LocalDate lastPaymentOrStart(Debt debt) {
        return paymentRepository.findAllByDebtIdOrderByPaidOnDesc(debt.getId()).stream()
                .findFirst()
                .map(DebtPayment::getPaidOn)
                .orElse(debt.getStartDate());
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressView progress(UUID userId, UUID debtId) {
        Debt debt = loadOwned(userId, debtId);
        List<DebtProgressCalculator.Row> remaining = scheduleRepository
                .findAllByDebtIdAndPaidPaymentIdIsNullOrderByDueDateAsc(debtId).stream()
                .map(r -> new DebtProgressCalculator.Row(r.getDueDate(), r.getInterest(), r.getFee()))
                .toList();
        DebtProgressCalculator.Result result = DebtProgressCalculator.calculate(
                new DebtProgressCalculator.Input(debt.getOriginalAmount(), debt.getOutstandingPrincipal(), remaining));
        return new ProgressView(result.paidPercent(), result.monthsLeft(), result.estimatedCloseDate(), result.interestRemaining());
    }

    @Override
    @Transactional(readOnly = true)
    public SimulateView simulate(UUID userId, UUID debtId, BigDecimal extraMonthly) {
        Debt debt = loadOwned(userId, debtId);
        LocalDate today = today(userId);
        DebtPayoffSimulator.Result result = DebtPayoffSimulator.simulate(new DebtPayoffSimulator.Input(
                debt.getOutstandingPrincipal(), debt.getAnnualRate(), debt.getInterestMethod(), debt.getMonthlyPayment(),
                extraMonthly, today, debt.getPaymentDay()));
        return new SimulateView(result.newCloseDate(), result.monthsSaved(), result.interestSaved());
    }

    @Override
    @Transactional(readOnly = true)
    public StrategyView strategy(UUID userId, BigDecimal extraMonthly) {
        LocalDate today = today(userId);
        List<DebtPayoffStrategy.DebtInput> inputs = debtRepository.findAllByUserIdAndStatus(userId, DebtStatus.ACTIVE).stream()
                .map(d -> new DebtPayoffStrategy.DebtInput(
                        d.getId(), d.getOutstandingPrincipal(), d.getAnnualRate(), d.getInterestMethod(),
                        d.getMonthlyPayment(), d.getPaymentDay(), today))
                .toList();
        if (inputs.isEmpty()) {
            StrategyOutcome empty = new StrategyOutcome(today, BigDecimal.ZERO.setScale(2));
            return new StrategyView(empty, empty, DebtPayoffStrategy.Strategy.AVALANCHE);
        }
        DebtPayoffStrategy.Comparison comparison = DebtPayoffStrategy.compare(inputs, extraMonthly);
        return new StrategyView(
                new StrategyOutcome(comparison.avalanche().closeDate(), comparison.avalanche().totalInterest()),
                new StrategyOutcome(comparison.snowball().closeDate(), comparison.snowball().totalInterest()),
                comparison.recommended());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal monthInterestAndFee(UUID userId, ZoneId zone, YearMonth month) {
        MonthDebtTotals totals = monthTotals(userId, zone, month);
        return totals.interestAndFee();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthDebtTotals monthTotals(UUID userId, ZoneId zone, YearMonth month) {
        List<UUID> debtIds = debtRepository.findAllByUserId(userId).stream().map(Debt::getId).toList();
        if (debtIds.isEmpty()) {
            return new MonthDebtTotals(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));
        }
        List<DebtPayment> payments =
                paymentRepository.findAllByDebtIdInAndPaidOnBetween(debtIds, month.atDay(1), month.atEndOfMonth());
        BigDecimal principal = payments.stream().map(DebtPayment::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interestAndFee = payments.stream()
                .map(p -> p.getInterest().add(p.getFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthDebtTotals(principal, interestAndFee);
    }

    private void saveGeneratedSchedule(UUID debtId, List<DebtScheduleGenerator.Row> rows) {
        List<DebtScheduleRow> entities = rows.stream()
                .map(r -> new DebtScheduleRow(debtId, r.seq(), r.dueDate(), r.principal(), r.interest(), r.fee(), true))
                .toList();
        scheduleRepository.saveAll(entities);
    }

    private int monthsBetween(LocalDate startDate, LocalDate endDate) {
        long months = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1));
        return (int) Math.max(1, months);
    }

    private LocalDate today(UUID userId) {
        ZoneId zone = userProfiles.zoneOf(userId);
        return LocalDate.now(clock.withZone(zone));
    }

    private Debt loadOwned(UUID userId, UUID debtId) {
        return OwnershipGuard.requireFound(debtRepository.findByIdAndUserId(debtId, userId), "debt");
    }

    private DebtView toView(Debt debt) {
        boolean estimated = scheduleRepository.findFirstByDebtIdAndPaidPaymentIdIsNullOrderByDueDateAsc(debt.getId())
                .map(DebtScheduleRow::isEstimated)
                .orElse(true);
        return new DebtView(
                debt.getId(), debt.getName(), debt.getLender(), debt.getType(), debt.getOriginalAmount(),
                debt.getOutstandingPrincipal(), debt.getAnnualRate(), debt.getMonthlyPayment(), debt.getStartDate(),
                debt.getEndDate(), debt.getPaymentDay(), debt.getInterestMethod(), debt.getStatus(), estimated);
    }

    private ScheduleRowView toRowView(DebtScheduleRow row) {
        return new ScheduleRowView(
                row.getId(), row.getSeq(), row.getDueDate(), row.getPrincipal(), row.getInterest(), row.getFee(),
                row.isEstimated(), row.isPaid());
    }

}
