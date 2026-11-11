package uz.zarflow.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface AccountRepository extends Repository<Account, UUID> {

    Account save(Account account);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    Optional<Account> findByIdAndUserIdAndArchivedAtIsNull(UUID id, UUID userId);

    List<Account> findAllByUserIdAndArchivedAtIsNull(UUID userId);

    boolean existsByUserIdAndIsDefaultTrueAndArchivedAtIsNull(UUID userId);

    Optional<Account> findByUserIdAndHumoCardLast4AndArchivedAtIsNull(UUID userId, String humoCardLast4);

    /**
     * EXPENSE and DEBT_PAYMENT rows are stored as a positive magnitude but subtract from
     * balance; INCOME and TRANSFER rows are already signed correctly for a plain add
     * (see V8__transactions.sql).
     */
    @Query(value = """
            SELECT a.id AS id,
                   a.initial_balance + COALESCE(SUM(
                       CASE WHEN t.type IN ('EXPENSE', 'DEBT_PAYMENT') THEN -t.amount ELSE t.amount END
                   ) FILTER (WHERE t.deleted_at IS NULL), 0) AS balance
            FROM accounts a
            LEFT JOIN transactions t ON t.account_id = a.id
            WHERE a.user_id = :userId
            GROUP BY a.id, a.initial_balance
            """, nativeQuery = true)
    List<AccountBalanceRow> findBalancesByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT a.initial_balance + COALESCE(SUM(
                       CASE WHEN t.type IN ('EXPENSE', 'DEBT_PAYMENT') THEN -t.amount ELSE t.amount END
                   ) FILTER (WHERE t.deleted_at IS NULL), 0)
            FROM accounts a
            LEFT JOIN transactions t ON t.account_id = a.id
            WHERE a.id = :accountId
            GROUP BY a.initial_balance
            """, nativeQuery = true)
    Optional<BigDecimal> findBalanceByAccountId(@Param("accountId") UUID accountId);

    interface AccountBalanceRow {
        UUID getId();

        BigDecimal getBalance();
    }

    /** Raw (non-aggregated) rows for {@link AccountBalanceMath#asOf} -- a Humo reconciliation
     * needs the account's balance as of an arbitrary past instant (the balance message's
     * as-of time), not just "now", so the cutoff is applied in pure Java instead of SQL. */
    @Query(value = """
            SELECT t.type AS type, t.amount AS amount, t.transaction_date AS transactionDate
            FROM transactions t
            WHERE t.account_id = :accountId AND t.deleted_at IS NULL
            """, nativeQuery = true)
    List<LedgerRow> findLedgerRowsByAccountId(@Param("accountId") UUID accountId);

    interface LedgerRow {
        String getType();

        BigDecimal getAmount();

        Instant getTransactionDate();
    }

}
