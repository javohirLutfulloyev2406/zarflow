package uz.zarflow.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
