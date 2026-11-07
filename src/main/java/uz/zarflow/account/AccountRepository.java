package uz.zarflow.account;

import java.math.BigDecimal;
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

    /**
     * EXPENSE rows are stored as a positive magnitude but subtract from balance;
     * INCOME and TRANSFER rows are already signed correctly for a plain add
     * (see V8__transactions.sql).
     */
    @Query(value = """
            SELECT a.id AS id,
                   a.initial_balance + COALESCE(SUM(
                       CASE WHEN t.type = 'EXPENSE' THEN -t.amount ELSE t.amount END
                   ) FILTER (WHERE t.deleted_at IS NULL), 0) AS balance
            FROM accounts a
            LEFT JOIN transactions t ON t.account_id = a.id
            WHERE a.user_id = :userId
            GROUP BY a.id, a.initial_balance
            """, nativeQuery = true)
    List<AccountBalanceRow> findBalancesByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT a.initial_balance + COALESCE(SUM(
                       CASE WHEN t.type = 'EXPENSE' THEN -t.amount ELSE t.amount END
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

}
