package uz.zarflow.account;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.shared.security.OwnershipGuard;

@Service
class AccountsService implements Accounts {

    private final AccountRepository accountRepository;
    private final Clock clock;

    AccountsService(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> listForUser(UUID userId) {
        List<Account> accounts = accountRepository.findAllByUserIdAndArchivedAtIsNull(userId);
        Map<UUID, BigDecimal> balances = accountRepository.findBalancesByUserId(userId).stream()
                .collect(Collectors.toMap(AccountRepository.AccountBalanceRow::getId,
                        AccountRepository.AccountBalanceRow::getBalance));
        return accounts.stream().map(account -> toView(account, balances.get(account.getId()))).toList();
    }

    @Override
    @Transactional
    public AccountView create(UUID userId, CreateAccount input) {
        if (input.isDefault() && accountRepository.existsByUserIdAndIsDefaultTrueAndArchivedAtIsNull(userId)) {
            accountRepository.findAllByUserIdAndArchivedAtIsNull(userId).stream()
                    .filter(Account::isDefault)
                    .forEach(Account::unmarkDefault);
        }
        Account account = new Account(
                userId, input.name(), input.type(), input.initialBalance(), input.color(), input.isDefault());
        accountRepository.save(account);
        return toView(account, input.initialBalance());
    }

    @Override
    @Transactional
    public AccountView update(UUID userId, UUID accountId, UpdateAccount input) {
        Account account = requireActive(userId, accountId);
        account.update(input.name(), input.type(), input.color());
        accountRepository.save(account);
        return toView(account, currentBalance(userId, accountId));
    }

    @Override
    @Transactional
    public AccountView archive(UUID userId, UUID accountId) {
        Account account = requireActive(userId, accountId);
        account.archive(Instant.now(clock));
        accountRepository.save(account);
        return toView(account, currentBalance(userId, accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountOwnership requireOwned(UUID userId, UUID accountId) {
        Account account = OwnershipGuard.requireFound(
                accountRepository.findByIdAndUserId(accountId, userId), "account");
        return new AccountOwnership(account.getId(), account.getCurrency());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal currentBalance(UUID userId, UUID accountId) {
        requireOwned(userId, accountId);
        return accountRepository.findBalanceByAccountId(accountId).orElse(BigDecimal.ZERO);
    }

    private Account requireActive(UUID userId, UUID accountId) {
        return OwnershipGuard.requireFound(
                accountRepository.findByIdAndUserIdAndArchivedAtIsNull(accountId, userId), "account");
    }

    private AccountView toView(Account account, BigDecimal balance) {
        return new AccountView(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getInitialBalance(),
                account.getColor(),
                account.isDefault(),
                balance == null ? account.getInitialBalance() : balance,
                account.getArchivedAt());
    }

}
