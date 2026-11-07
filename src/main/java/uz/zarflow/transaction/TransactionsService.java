package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    TransactionsService(
            TransactionRepository transactionRepository,
            TransactionAuditRepository auditRepository,
            Accounts accounts,
            Categories categories,
            Clock clock,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
        this.accounts = accounts;
        this.categories = categories;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    TransactionView create(UUID userId, CreateCommand cmd) {
        if (cmd.idempotencyKey() != null) {
            var existing = transactionRepository.findByUserIdAndIdempotencyKey(userId, cmd.idempotencyKey());
            if (existing.isPresent()) {
                return toView(existing.get());
            }
        }
        if (cmd.type() == TransactionType.TRANSFER) {
            throw new BusinessRuleException("transaction.use_transfer_endpoint");
        }
        Instant transactionDate = cmd.transactionDate() == null ? Instant.now(clock) : cmd.transactionDate();
        requireNotFuture(transactionDate);
        accounts.requireOwned(userId, cmd.accountId());
        Categories.CategoryOwnership category = categories.requireUsable(userId, cmd.categoryId());
        requireCategoryMatchesType(cmd.type(), category.type());
        Necessity necessity = resolveNecessity(cmd.type(), cmd.necessity(), category.necessity());

        Transaction transaction = new Transaction(
                userId, cmd.accountId(), cmd.categoryId(), cmd.type(), cmd.amount(), necessity,
                transactionDate, cmd.description(), TransactionSource.MINI_APP, null, cmd.idempotencyKey());
        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ex) {
            if (cmd.idempotencyKey() == null) {
                throw ex;
            }
            return transactionRepository.findByUserIdAndIdempotencyKey(userId, cmd.idempotencyKey())
                    .map(this::toView)
                    .orElseThrow(() -> ex);
        }
        audit(transaction.getId(), TransactionAuditAction.CREATE, null, transaction);
        return toView(transaction);
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

        TransactionView before = toView(transaction);
        transaction.update(cmd.accountId(), cmd.categoryId(), cmd.amount(), necessity, transactionDate, cmd.description());
        transactionRepository.save(transaction);
        audit(transaction.getId(), TransactionAuditAction.UPDATE, before, toView(transaction));
        return toView(transaction);
    }

    @Transactional
    void delete(UUID userId, UUID id) {
        Transaction transaction = requireOwned(userId, id);
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
                transactionDate, cmd.description(), TransactionSource.MINI_APP, transferGroupId, null);
        Transaction incoming = new Transaction(
                userId, cmd.toAccountId(), null, TransactionType.TRANSFER, cmd.amount(), null,
                transactionDate, cmd.description(), TransactionSource.MINI_APP, transferGroupId, null);
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
                description == null ? "Balans tuzatish" : description, TransactionSource.MINI_APP, null, null);
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

        return new TodayAndMonth(todayExpenses, todayTotal, monthIncome, monthExpense, monthIncome.subtract(monthExpense));
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

    private TransactionView toView(Transaction t) {
        return new TransactionView(
                t.getId(), t.getAccountId(), t.getCategoryId(), t.getType(), t.getAmount(), t.getCurrency(),
                t.getNecessity(), t.getTransactionDate(), t.getDescription(), t.getTransferGroupId(),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    record CreateCommand(
            UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount, Necessity necessity,
            Instant transactionDate, String description, String idempotencyKey) {
    }

    record UpdateCommand(
            UUID accountId, UUID categoryId, BigDecimal amount, Necessity necessity,
            Instant transactionDate, String description) {
    }

    record SearchCommand(Instant from, Instant to, TransactionType type, UUID categoryId, UUID accountId, String q) {
    }

    record TransferCommand(UUID fromAccountId, UUID toAccountId, BigDecimal amount, Instant transactionDate, String description) {
    }

    record TransactionView(
            UUID id, UUID accountId, UUID categoryId, TransactionType type, BigDecimal amount, String currency,
            Necessity necessity, Instant transactionDate, String description, UUID transferGroupId,
            Instant createdAt, Instant updatedAt) {
    }

    record TransferResult(UUID transferGroupId, TransactionView fromLeg, TransactionView toLeg) {
    }

    record PageResult(List<TransactionView> items, long totalElements, int page, int size) {
    }

}
