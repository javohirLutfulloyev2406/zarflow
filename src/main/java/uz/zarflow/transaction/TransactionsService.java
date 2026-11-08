package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uz.zarflow.account.Accounts;
import uz.zarflow.category.CategoryType;
import uz.zarflow.category.Categories;
import uz.zarflow.category.Necessity;
import uz.zarflow.shared.common.DateRange;
import uz.zarflow.shared.exception.BusinessRuleException;
import uz.zarflow.shared.security.OwnershipGuard;

@Service
class TransactionsService implements Transactions {

    private static final Logger log = LoggerFactory.getLogger(TransactionsService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionAuditRepository auditRepository;
    private final Accounts accounts;
    private final Categories categories;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    TransactionsService(
            TransactionRepository transactionRepository,
            TransactionAuditRepository auditRepository,
            Accounts accounts,
            Categories categories,
            Clock clock,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
        this.accounts = accounts;
        this.categories = categories;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    TransactionView create(UUID userId, CreateCommand cmd) {
        return createInternal(
                userId, cmd.accountId(), cmd.categoryId(), cmd.type(), cmd.amount(), cmd.necessity(),
                cmd.transactionDate(), cmd.description(), TransactionSource.MINI_APP, cmd.idempotencyKey(), cmd.isFixed());
    }

    @Override
    @Transactional
    public BotTransaction createFromBot(UUID userId, BotCreateInput input) {
        TransactionView view = createInternal(
                userId, input.accountId(), input.categoryId(), input.type(), input.amount(), input.necessity(),
                null, input.description(), TransactionSource.BOT, input.idempotencyKey(), null);
        return toBotTransaction(view);
    }

    @Override
    @Transactional
    public BotTransaction createFromRecurring(UUID userId, RecurringCreateInput input) {
        TransactionView view = createInternal(
                userId, input.accountId(), input.categoryId(), input.type(), input.amount(),
                null, null, input.description(), TransactionSource.RECURRING, input.idempotencyKey(), input.isFixed());
        return toBotTransaction(view);
    }

    private TransactionView createInternal(
            UUID userId, UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount,
            Necessity necessity, Instant requestedDate, String description, TransactionSource source,
            String idempotencyKey, Boolean isFixedOverride) {
        if (idempotencyKey != null) {
            var existing = transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return toView(existing.get());
            }
        }
        if (type == TransactionType.TRANSFER) {
            throw new BusinessRuleException("transaction.use_transfer_endpoint");
        }
        Instant transactionDate = requestedDate == null ? Instant.now(clock) : requestedDate;
        requireNotFuture(transactionDate);
        accounts.requireOwned(userId, accountId);

        Necessity resolvedNecessity;
        boolean isFixed;
        if (type == TransactionType.DEBT_PAYMENT) {
            resolvedNecessity = null;
            isFixed = isFixedOverride != null ? isFixedOverride : false;
        } else {
            Categories.CategoryOwnership category = categories.requireUsable(userId, categoryId);
            requireCategoryMatchesType(type, category.type());
            resolvedNecessity = resolveNecessity(type, necessity, category.necessity());
            isFixed = isFixedOverride != null ? isFixedOverride : category.fixedDefault();
        }

        Transaction transaction = new Transaction(
                userId, accountId, categoryId, type, amount, resolvedNecessity,
                transactionDate, description, source, null, idempotencyKey, isFixed);
        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ex) {
            if (idempotencyKey == null) {
                throw ex;
            }
            return transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .map(this::toView)
                    .orElseThrow(() -> ex);
        }
        audit(transaction.getId(), TransactionAuditAction.CREATE, null, transaction);
        if (type == TransactionType.EXPENSE) {
            eventPublisher.publishEvent(new ExpenseRecorded(userId, categoryId));
        }
        return toView(transaction);
    }

    @Override
    @Transactional
    public BotTransaction updateCategory(UUID userId, UUID transactionId, UUID categoryId) {
        Transaction transaction = requireOwned(userId, transactionId);
        if (transaction.getType() == TransactionType.TRANSFER) {
            throw new BusinessRuleException("transaction.transfer_not_editable");
        }
        Categories.CategoryOwnership category = categories.requireUsable(userId, categoryId);
        requireCategoryMatchesType(transaction.getType(), category.type());
        Necessity necessity = resolveNecessity(transaction.getType(), null, category.necessity());

        TransactionView before = toView(transaction);
        transaction.update(transaction.getAccountId(), categoryId, transaction.getAmount(), necessity,
                transaction.getTransactionDate(), transaction.getDescription(), transaction.isFixed());
        transactionRepository.save(transaction);
        audit(transaction.getId(), TransactionAuditAction.UPDATE, before, toView(transaction));
        if (transaction.getType() == TransactionType.EXPENSE) {
            eventPublisher.publishEvent(new ExpenseRecorded(userId, categoryId));
        }
        return toBotTransaction(toView(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotTransaction> listToday(UUID userId, ZoneId zone) {
        DateRange.InstantRange today = DateRange.today(clock, zone).toInstantRange(zone);
        return transactionRepository
                .findIncomeAndExpenseInRange(userId, today.startInclusive(), today.endExclusive()).stream()
                .map(t -> toBotTransaction(toView(t)))
                .toList();
    }

    @Override
    @Transactional
    public Optional<BotTransaction> undoLast(UUID userId) {
        Instant cutoff = Instant.now(clock).minus(Duration.ofHours(24));
        Optional<Transaction> lastOpt =
                transactionRepository.findTopByUserIdAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(userId, cutoff);
        if (lastOpt.isEmpty()) {
            return Optional.empty();
        }
        Transaction last = lastOpt.get();
        BotTransaction result = toBotTransaction(toView(last));
        if (last.getType() == TransactionType.TRANSFER && last.getTransferGroupId() != null) {
            transactionRepository.findByTransferGroupIdAndDeletedAtIsNull(last.getTransferGroupId())
                    .forEach(this::softDeleteEntity);
        } else {
            softDeleteEntity(last);
        }
        return Optional.of(result);
    }

    @Transactional
    TransactionView update(UUID userId, UUID id, UpdateCommand cmd) {
        Transaction transaction = requireOwned(userId, id);
        Instant transactionDate = cmd.transactionDate() == null ? transaction.getTransactionDate() : cmd.transactionDate();
        requireNotFuture(transactionDate);
        accounts.requireOwned(userId, cmd.accountId());

        Necessity necessity;
        if (transaction.getType() == TransactionType.TRANSFER) {
            throw new BusinessRuleException("transaction.transfer_not_editable");
        } else if (transaction.getType() == TransactionType.EXPENSE || transaction.getType() == TransactionType.INCOME) {
            Categories.CategoryOwnership category = categories.requireUsable(userId, cmd.categoryId());
            requireCategoryMatchesType(transaction.getType(), category.type());
            necessity = resolveNecessity(transaction.getType(), cmd.necessity(), category.necessity());
        } else {
            necessity = null;
        }

        boolean isFixed = cmd.isFixed() != null ? cmd.isFixed() : transaction.isFixed();

        TransactionView before = toView(transaction);
        transaction.update(cmd.accountId(), cmd.categoryId(), cmd.amount(), necessity, transactionDate, cmd.description(), isFixed);
        transactionRepository.save(transaction);
        audit(transaction.getId(), TransactionAuditAction.UPDATE, before, toView(transaction));
        if (transaction.getType() == TransactionType.EXPENSE) {
            eventPublisher.publishEvent(new ExpenseRecorded(userId, transaction.getCategoryId()));
        }
        return toView(transaction);
    }

    @Override
    @Transactional
    public void softDelete(UUID userId, UUID id) {
        softDeleteEntity(requireOwned(userId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BotTransaction> find(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserIdAndDeletedAtIsNull(transactionId, userId)
                .map(t -> toBotTransaction(toView(t)));
    }

    private void softDeleteEntity(Transaction transaction) {
        TransactionView before = toView(transaction);
        transaction.softDelete(Instant.now(clock));
        transactionRepository.save(transaction);
        audit(transaction.getId(), TransactionAuditAction.DELETE, before, toView(transaction));
    }

    @Transactional(readOnly = true)
    PageResult list(UUID userId, SearchCommand cmd, Pageable pageable) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (cmd.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), cmd.from()));
            }
            if (cmd.to() != null) {
                predicates.add(cb.lessThan(root.get("transactionDate"), cmd.to()));
            }
            if (cmd.type() != null) {
                predicates.add(cb.equal(root.get("type"), cmd.type()));
            }
            if (cmd.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), cmd.categoryId()));
            }
            if (cmd.accountId() != null) {
                predicates.add(cb.equal(root.get("accountId"), cmd.accountId()));
            }
            if (cmd.q() != null && !cmd.q().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + cmd.q().toLowerCase() + "%"));
            }
            query.orderBy(cb.desc(root.get("transactionDate")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        return new PageResult(
                page.getContent().stream().map(this::toView).toList(),
                page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Transactional
    TransferResult transfer(UUID userId, TransferCommand cmd) {
        if (cmd.fromAccountId().equals(cmd.toAccountId())) {
            throw new BusinessRuleException("transaction.transfer_same_account");
        }
        accounts.requireOwned(userId, cmd.fromAccountId());
        accounts.requireOwned(userId, cmd.toAccountId());
        Instant transactionDate = cmd.transactionDate() == null ? Instant.now(clock) : cmd.transactionDate();
        requireNotFuture(transactionDate);

        UUID transferGroupId = UUID.randomUUID();
        Transaction outgoing = new Transaction(
                userId, cmd.fromAccountId(), null, TransactionType.TRANSFER, cmd.amount().negate(), null,
                transactionDate, cmd.description(), TransactionSource.MINI_APP, transferGroupId, null, false);
        Transaction incoming = new Transaction(
                userId, cmd.toAccountId(), null, TransactionType.TRANSFER, cmd.amount(), null,
                transactionDate, cmd.description(), TransactionSource.MINI_APP, transferGroupId, null, false);
        transactionRepository.save(outgoing);
        transactionRepository.save(incoming);
        audit(outgoing.getId(), TransactionAuditAction.CREATE, null, outgoing);
        audit(incoming.getId(), TransactionAuditAction.CREATE, null, incoming);
        return new TransferResult(transferGroupId, toView(outgoing), toView(incoming));
    }

    @Transactional
    TransactionView adjust(UUID userId, UUID accountId, BigDecimal actualBalance, String description) {
        accounts.requireOwned(userId, accountId);
        BigDecimal currentBalance = accounts.currentBalance(userId, accountId);
        BigDecimal delta = actualBalance.subtract(currentBalance);
        if (delta.signum() == 0) {
            throw new BusinessRuleException("transaction.adjust_no_difference");
        }
        TransactionType type = delta.signum() > 0 ? TransactionType.INCOME : TransactionType.EXPENSE;
        BigDecimal amount = delta.abs();
        Transaction transaction = new Transaction(
                userId, accountId, null, type, amount, null, Instant.now(clock),
                description == null ? "Balans tuzatish" : description, TransactionSource.MINI_APP, null, null, false);
        transactionRepository.save(transaction);
        audit(transaction.getId(), TransactionAuditAction.CREATE, null, transaction);
        return toView(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TodayAndMonth summarize(UUID userId, ZoneId zone) {
        DateRange.InstantRange today = DateRange.today(clock, zone).toInstantRange(zone);
        DateRange.InstantRange month = DateRange.thisMonth(clock, zone).toInstantRange(zone);

        List<ExpenseLine> todayExpenses = transactionRepository
                .findExpensesInRange(userId, today.startInclusive(), today.endExclusive()).stream()
                .map(t -> new ExpenseLine(t.getId(), t.getAccountId(), t.getCategoryId(), t.getAmount(),
                        t.getDescription(), t.getTransactionDate()))
                .toList();
        BigDecimal todayTotal = todayExpenses.stream().map(ExpenseLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<TransactionType, BigDecimal> monthTotals = transactionRepository
                .sumByTypeInRange(userId, month.startInclusive(), month.endExclusive()).stream()
                .collect(Collectors.toMap(TransactionRepository.TypeTotalRow::getType, TransactionRepository.TypeTotalRow::getTotal));
        BigDecimal monthIncome = monthTotals.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO);
        BigDecimal monthExpense = monthTotals.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO);
        BigDecimal monthDebtPayments = monthTotals.getOrDefault(TransactionType.DEBT_PAYMENT, BigDecimal.ZERO);

        return new TodayAndMonth(
                todayExpenses, todayTotal, monthIncome, monthExpense, monthIncome.subtract(monthExpense), monthDebtPayments);
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodAnalytics analyticsForRange(UUID userId, ZoneId zone, LocalDate from, LocalDate toInclusive) {
        DateRange.InstantRange range = DateRange.custom(from, toInclusive).toInstantRange(zone);
        List<Transaction> rangeTx = transactionRepository
                .findIncomeAndExpenseInRange(userId, range.startInclusive(), range.endExclusive());

        BigDecimal income = sumAmount(rangeTx, TransactionType.INCOME, null);
        BigDecimal expense = sumAmount(rangeTx, TransactionType.EXPENSE, null);
        BigDecimal essential = sumAmount(rangeTx, TransactionType.EXPENSE, Necessity.ESSENTIAL);
        BigDecimal discretionary = sumAmount(rangeTx, TransactionType.EXPENSE, Necessity.DISCRETIONARY);
        List<CategoryAmount> expenseByCategory = expenseByCategory(rangeTx).entrySet().stream()
                .map(e -> new CategoryAmount(e.getKey(), e.getValue()))
                .toList();

        return new PeriodAnalytics(income, expense, essential, discretionary, expenseByCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DayTotal> dailyTotalsForMonth(UUID userId, ZoneId zone, YearMonth month) {
        DateRange.InstantRange range = DateRange.ofMonth(month).toInstantRange(zone);
        List<Transaction> monthTx = transactionRepository
                .findIncomeAndExpenseInRange(userId, range.startInclusive(), range.endExclusive());

        Map<LocalDate, List<Transaction>> byDay =
                monthTx.stream().collect(Collectors.groupingBy(t -> t.getTransactionDate().atZone(zone).toLocalDate()));

        return byDay.entrySet().stream()
                .map(e -> new DayTotal(
                        e.getKey(),
                        sumAmount(e.getValue(), TransactionType.INCOME, null),
                        sumAmount(e.getValue(), TransactionType.EXPENSE, null),
                        e.getValue().size()))
                .sorted(Comparator.comparing(DayTotal::date))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DayDetail dayDetail(UUID userId, ZoneId zone, LocalDate date) {
        List<Transaction> dayTx = findInLocalDay(userId, zone, date);
        List<BotTransaction> views = dayTx.stream().map(t -> toBotTransaction(toView(t))).toList();
        return new DayDetail(views, sumAmount(dayTx, TransactionType.INCOME, null), sumAmount(dayTx, TransactionType.EXPENSE, null));
    }

    @Override
    @Transactional(readOnly = true)
    public DailyDigest dailyDigest(UUID userId, ZoneId zone, LocalDate date) {
        List<Transaction> dayTx = findInLocalDay(userId, zone, date);
        BigDecimal income = sumAmount(dayTx, TransactionType.INCOME, null);
        BigDecimal expense = sumAmount(dayTx, TransactionType.EXPENSE, null);

        Map.Entry<UUID, BigDecimal> top = expenseByCategory(dayTx).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        return new DailyDigest(income, expense, dayTx.size(), top == null ? null : top.getKey(), top == null ? null : top.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthEntry> monthEntries(UUID userId, ZoneId zone, YearMonth month) {
        DateRange.InstantRange range = DateRange.ofMonth(month).toInstantRange(zone);
        return transactionRepository.findIncomeAndExpenseInRange(userId, range.startInclusive(), range.endExclusive()).stream()
                .map(t -> new MonthEntry(t.getType(), t.getAmount(), t.isFixed(), t.getCategoryId(), t.getTransactionDate()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal monthDebtPayments(UUID userId, ZoneId zone, YearMonth month) {
        DateRange.InstantRange range = DateRange.ofMonth(month).toInstantRange(zone);
        return transactionRepository.sumByTypeInRange(userId, range.startInclusive(), range.endExclusive()).stream()
                .filter(row -> row.getType() == TransactionType.DEBT_PAYMENT)
                .map(TransactionRepository.TypeTotalRow::getTotal)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<LocalDate> activeDates(UUID userId, ZoneId zone, LocalDate from, LocalDate toInclusive) {
        DateRange.InstantRange range = DateRange.custom(from, toInclusive).toInstantRange(zone);
        return transactionRepository.findIncomeAndExpenseInRange(userId, range.startInclusive(), range.endExclusive())
                .stream()
                .map(t -> t.getTransactionDate().atZone(zone).toLocalDate())
                .collect(Collectors.toSet());
    }

    private List<Transaction> findInLocalDay(UUID userId, ZoneId zone, LocalDate date) {
        DateRange.InstantRange range = DateRange.custom(date, date).toInstantRange(zone);
        return transactionRepository.findIncomeAndExpenseInRange(userId, range.startInclusive(), range.endExclusive());
    }

    private Map<UUID, BigDecimal> expenseByCategory(List<Transaction> list) {
        return list.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategoryId, Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
    }

    private BigDecimal sumAmount(List<Transaction> list, TransactionType type, Necessity necessity) {
        return list.stream()
                .filter(t -> t.getType() == type)
                .filter(t -> necessity == null || t.getNecessity() == necessity)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Transaction requireOwned(UUID userId, UUID id) {
        return OwnershipGuard.requireFound(
                transactionRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId), "transaction");
    }

    private void requireNotFuture(Instant transactionDate) {
        if (transactionDate.isAfter(Instant.now(clock))) {
            throw new BusinessRuleException("transaction.future_date");
        }
    }

    private void requireCategoryMatchesType(TransactionType type, CategoryType categoryType) {
        boolean matches = (type == TransactionType.EXPENSE && categoryType == CategoryType.EXPENSE)
                || (type == TransactionType.INCOME && categoryType == CategoryType.INCOME);
        if (!matches) {
            throw new BusinessRuleException("transaction.category_type_mismatch");
        }
    }

    private Necessity resolveNecessity(TransactionType type, Necessity requested, Necessity categoryDefault) {
        if (type != TransactionType.EXPENSE) {
            return null;
        }
        return requested != null ? requested : categoryDefault;
    }

    private void audit(UUID transactionId, TransactionAuditAction action, Object before, Object after) {
        auditRepository.save(new TransactionAudit(transactionId, action, toJson(before), toJson(after)));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        Object payload = value instanceof Transaction t ? toView(t) : value;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize transaction audit payload", ex);
            return null;
        }
    }

    private BotTransaction toBotTransaction(TransactionView view) {
        return new BotTransaction(
                view.id(), view.accountId(), view.categoryId(), view.type(), view.amount(),
                view.description(), view.transactionDate());
    }

    private TransactionView toView(Transaction t) {
        return new TransactionView(
                t.getId(), t.getAccountId(), t.getCategoryId(), t.getType(), t.getAmount(), t.getCurrency(),
                t.getNecessity(), t.getTransactionDate(), t.getDescription(), t.getTransferGroupId(),
                t.getCreatedAt(), t.getUpdatedAt(), t.isFixed());
    }

    record CreateCommand(
            UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount, Necessity necessity,
            Instant transactionDate, String description, String idempotencyKey, Boolean isFixed) {
    }

    record UpdateCommand(
            UUID accountId, UUID categoryId, BigDecimal amount, Necessity necessity,
            Instant transactionDate, String description, Boolean isFixed) {
    }

    record SearchCommand(Instant from, Instant to, TransactionType type, UUID categoryId, UUID accountId, String q) {
    }

    record TransferCommand(UUID fromAccountId, UUID toAccountId, BigDecimal amount, Instant transactionDate, String description) {
    }

    record TransactionView(
            UUID id, UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount, String currency,
            Necessity necessity, Instant transactionDate, String description, UUID transferGroupId,
            Instant createdAt, Instant updatedAt, boolean isFixed) {
    }

    record TransferResult(UUID transferGroupId, TransactionView fromLeg, TransactionView toLeg) {
    }

    record PageResult(List<TransactionView> items, long totalElements, int page, int size) {
    }

}
