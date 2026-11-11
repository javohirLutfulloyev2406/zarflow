package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Extends {@link JpaSpecificationExecutor} for {@link TransactionsService#list} — the
 * search filters are all optional, and Postgres cannot infer a bind parameter's type
 * when its only use is "? IS NULL" with no other typed context in the query (fails with
 * "could not determine data type of parameter"). Specifications sidestep this entirely
 * by omitting a predicate (and its parameter) for each filter that wasn't supplied,
 * rather than always binding it and branching on null in SQL.
 */
interface TransactionRepository extends Repository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Transaction save(Transaction transaction);

    Optional<Transaction> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Transaction> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<Transaction> findTopByUserIdAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID userId, Instant createdAfter);

    List<Transaction> findByTransferGroupIdAndDeletedAtIsNull(UUID transferGroupId);

    /** {@code excludedExpenseCategoryId}/{@code excludedIncomeCategoryId} drop Humo
     * balance-reconciliation corrections (docs/PRD.md §7, system categories tuzatish_exp/
     * tuzatish_inc) -- real balance-affecting transactions, but never counted as analytics/
     * insights/daily-limit spend or income. Pass null for either to exclude nothing (e.g. the
     * categories don't exist yet in a fresh environment). */
    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.deletedAt is null and t.type in ('INCOME', 'EXPENSE')
              and t.transactionDate >= :from and t.transactionDate < :to
              and (:excludedExpenseCategoryId is null or t.categoryId <> :excludedExpenseCategoryId)
              and (:excludedIncomeCategoryId is null or t.categoryId <> :excludedIncomeCategoryId)
            order by t.transactionDate desc
            """)
    List<Transaction> findIncomeAndExpenseInRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to,
            @Param("excludedExpenseCategoryId") UUID excludedExpenseCategoryId,
            @Param("excludedIncomeCategoryId") UUID excludedIncomeCategoryId);

    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.deletedAt is null and t.type = 'EXPENSE'
              and t.transactionDate >= :from and t.transactionDate < :to
              and (:excludedExpenseCategoryId is null or t.categoryId <> :excludedExpenseCategoryId)
            order by t.transactionDate desc
            """)
    List<Transaction> findExpensesInRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to,
            @Param("excludedExpenseCategoryId") UUID excludedExpenseCategoryId);

    @Query("""
            select t.type as type, coalesce(sum(t.amount), 0) as total
            from Transaction t
            where t.userId = :userId and t.deletedAt is null and t.type in ('INCOME', 'EXPENSE', 'DEBT_PAYMENT')
              and t.transactionDate >= :from and t.transactionDate < :to
              and (:excludedExpenseCategoryId is null or t.categoryId <> :excludedExpenseCategoryId)
              and (:excludedIncomeCategoryId is null or t.categoryId <> :excludedIncomeCategoryId)
            group by t.type
            """)
    List<TypeTotalRow> sumByTypeInRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to,
            @Param("excludedExpenseCategoryId") UUID excludedExpenseCategoryId,
            @Param("excludedIncomeCategoryId") UUID excludedIncomeCategoryId);

    interface TypeTotalRow {
        TransactionType getType();

        BigDecimal getTotal();
    }

    /** The Humo import module's "possible manual duplicate" check (docs/PRD.md §5) --
     * {@code source != 'BANK_IMPORT'} so an import is never flagged as a duplicate of
     * another import (those are deduped by fingerprint instead, before this ever runs). */
    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.deletedAt is null and t.type = :type
              and t.amount = :amount and t.source <> 'BANK_IMPORT'
              and t.transactionDate >= :from and t.transactionDate <= :to
            order by t.transactionDate desc
            """)
    List<Transaction> findPossibleManualDuplicate(
            @Param("userId") UUID userId, @Param("type") TransactionType type, @Param("amount") BigDecimal amount,
            @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.accountId = :accountId and t.linkedGoalId = :goalId
              and t.deletedAt is null
            """)
    BigDecimal sumLinkedGoalAmount(@Param("userId") UUID userId, @Param("accountId") UUID accountId, @Param("goalId") UUID goalId);

    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.accountId = :accountId and t.linkedGoalId = :goalId
              and t.deletedAt is null
            order by t.transactionDate desc
            """)
    List<Transaction> findLinkedGoalHistory(
            @Param("userId") UUID userId, @Param("accountId") UUID accountId, @Param("goalId") UUID goalId);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.linkedGoalId is not null and t.accountId in :savingsAccountIds
              and t.deletedAt is null and t.transactionDate >= :from and t.transactionDate < :to
            """)
    BigDecimal sumGoalSavingsInRange(
            @Param("userId") UUID userId, @Param("savingsAccountIds") Collection<UUID> savingsAccountIds,
            @Param("from") Instant from, @Param("to") Instant to);

}
