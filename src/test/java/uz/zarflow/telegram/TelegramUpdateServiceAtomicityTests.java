package uz.zarflow.telegram;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.telegram.telegrambots.meta.api.objects.Update;

import uz.zarflow.TestcontainersConfiguration;
import uz.zarflow.user.UserAccounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression test for the bug where claimUpdate's insert committed independently
 * of registerOrUpdate: if the latter failed, the update_id was permanently marked
 * "processed" despite never actually being handled, with no way to reprocess it.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TelegramUpdateServiceAtomicityTests {

    @Autowired
    private TelegramUpdateService telegramUpdateService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class FailingUserAccountsConfig {
        @Bean
        @Primary
        UserAccounts failingUserAccounts() {
            return profile -> {
                throw new RuntimeException("simulated registration failure");
            };
        }
    }

    @Test
    void failedRegistrationRollsBackTheIdempotencyClaim() throws Exception {
        long updateId = ThreadLocalRandom.current().nextLong(1_000_000, 999_999_999);
        long chatId = ThreadLocalRandom.current().nextLong(1_000_000, 999_999_999);
        String body = TelegramUpdateTestSupport.privateMessageUpdate(
                updateId, chatId, chatId, "Ali", null, null, "uz", "/start");
        Update update = objectMapper.readValue(body, Update.class);

        assertThatThrownBy(() -> telegramUpdateService.handle(update))
                .isInstanceOf(RuntimeException.class);

        Integer claimed = jdbcTemplate.queryForObject(
                "select count(*) from telegram_updates where update_id = ?", Integer.class, updateId);
        assertThat(claimed).isZero();
    }

}
