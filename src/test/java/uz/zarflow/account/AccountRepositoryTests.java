package uz.zarflow.account;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import uz.zarflow.IntegrationTest;
import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AccountRepositoryTests {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID createUser() {
        return jdbcTemplate.queryForObject(
                "insert into users (telegram_id) values (?) returning id",
                UUID.class,
                ThreadLocalRandom.current().nextLong());
    }

    @Test
    void roundTripsInitialBalanceAtExactScale() {
        UUID userId = createUser();
        Account account = new Account(userId, "Main card", AccountType.BANK_CARD,
                new BigDecimal("1234567.89"), "#174D40", false);

        Account saved = accountRepository.save(account);

        Account reloaded = accountRepository.findByIdAndUserId(saved.getId(), userId).orElseThrow();
        assertThat(reloaded.getInitialBalance()).isEqualByComparingTo("1234567.89");
        assertThat(reloaded.getInitialBalance().scale()).isEqualTo(2);
    }

    @Test
    void doesNotReturnAnotherUsersAccount() {
        UUID userA = createUser();
        UUID userB = createUser();
        Account account = accountRepository.save(
                new Account(userA, "Cash", AccountType.CASH, BigDecimal.ZERO, null, false));

        assertThat(accountRepository.findByIdAndUserId(account.getId(), userB)).isEmpty();
    }

    @Test
    void secondDefaultAccountForSameUserIsRejected() {
        UUID userId = createUser();
        accountRepository.save(new Account(userId, "Main", AccountType.CASH, BigDecimal.ZERO, null, true));

        assertThatThrownBy(() -> accountRepository.save(
                new Account(userId, "Second", AccountType.CASH, BigDecimal.ZERO, null, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
