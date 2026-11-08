package uz.zarflow.transaction;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.deletedAt is null and t.type in ('INCOME', 'EXPENSE')
              and t.transactionDate >= :from and t.transactionDate < :to
            order by t.transactionDate desc
            """)
    List<Transaction> findIncomeAndExpenseInRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.deletedAt is null and t.type = 'EXPENSE'
              and t.transactionDate >= :from and t.transactionDate < :to
            order by t.transactionDate desc
            """)
    List<Transaction> findExpensesInRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select t.type as type, coalesce(sum(t.amount), 0) as total
            from Transaction t
            where t.userId = :userId and t.deletedAt is null and t.type in ('INCOME', 'EXPENSE', 'DEBT_PAYMENT')
              and t.transactionDate >= :from and t.transactionDate < :to
            group by t.type
            """)
    List<TypeTotalRow> sumByTypeInRange(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    interface TypeTotalRow {
        TransactionType getType();

        BigDecimal getTotal();
    }

}
