package uz.zarflow.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The account module's public API. Other modules (transaction's validation,
 * user's onboarding) talk to accounts only through here — never through
 * {@code Account}/{@code AccountRepository}.
 */
public interface Accounts {

    List<AccountView> listForUser(UUID userId);

    AccountView create(UUID userId, CreateAccount input);

    AccountView update(UUID userId, UUID accountId, UpdateAccount input);

    AccountView archive(UUID userId, UUID accountId);

    /** 404s (never 403) when the account doesn't exist or belongs to someone else. */
    AccountOwnership requireOwned(UUID userId, UUID accountId);

    BigDecimal currentBalance(UUID userId, UUID accountId);

    /** This account's balance as of an arbitrary past instant -- Humo reconciliation
     * (docs/PRD.md §7) compares against the balance message's own as-of time, not "now". */
    BigDecimal balanceAsOf(UUID userId, UUID accountId, Instant asOf);

    /** Empty when no account of this user has this Humo card linked. */
    Optional<UUID> findAccountIdByHumoCard(UUID userId, String cardLast4);

    /** Links a Humo card's last 4 digits to this account so future balance messages
     * reconcile against it; 404s (never 403) if the account isn't this user's. */
    void linkHumoCard(UUID userId, UUID accountId, String cardLast4);

    record CreateAccount(String name, AccountType type, BigDecimal initialBalance, String color, boolean isDefault) {
    }

    record UpdateAccount(String name, AccountType type, String color) {
    }

    record AccountView(
            UUID id,
            String name,
            AccountType type,
            String currency,
            BigDecimal initialBalance,
            String color,
            boolean isDefault,
            BigDecimal balance,
            Instant archivedAt) {
    }

    record AccountOwnership(UUID id, String currency) {
    }

}
