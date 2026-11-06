package uz.zarflow.identity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uz.zarflow.IntegrationTest;
import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TelegramLoginIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${zarflow.auth.bot-token}")
    private String botToken;

    @Test
    void validLoginCreatesUserAndReturnsToken() {
        long telegramId = uniqueTelegramId();
        String initData = sign(telegramId, "Ali", "Valiyev", "alivaliyev", "uz");

        ResponseEntity<LoginResponseBody> response = restTemplate.postForEntity(
                "/api/v1/auth/telegram", new LoginRequestBody(initData), LoginResponseBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponseBody body = response.getBody();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.tokenType()).isEqualTo("Bearer");
        assertThat(body.user().isNew()).isTrue();

        Integer userCount = jdbcTemplate.queryForObject(
                "select count(*) from users where telegram_id = ?", Integer.class, telegramId);
        assertThat(userCount).isEqualTo(1);
    }

    @Test
    void secondLoginReturnsSameUserUpdatesNamesKeepsLanguage() {
        long telegramId = uniqueTelegramId();

        ResponseEntity<LoginResponseBody> first = restTemplate.postForEntity(
                "/api/v1/auth/telegram",
                new LoginRequestBody(sign(telegramId, "Ali", "Valiyev", "alivaliyev", "uz")),
                LoginResponseBody.class);
        UUID firstUserId = first.getBody().user().id();

        ResponseEntity<LoginResponseBody> second = restTemplate.postForEntity(
                "/api/v1/auth/telegram",
                new LoginRequestBody(sign(telegramId, "Alisher", "Valiyev", "alivaliyev", "ru")),
                LoginResponseBody.class);
        UserBody secondUser = second.getBody().user();

        assertThat(secondUser.id()).isEqualTo(firstUserId);
        assertThat(secondUser.isNew()).isFalse();
        assertThat(secondUser.firstName()).isEqualTo("Alisher");

        String storedFirstName = jdbcTemplate.queryForObject(
                "select first_name from users where telegram_id = ?", String.class, telegramId);
        assertThat(storedFirstName).isEqualTo("Alisher");

        String storedLanguage = jdbcTemplate.queryForObject(
                "select language from users where telegram_id = ?", String.class, telegramId);
        assertThat(storedLanguage).isEqualTo("uz");
    }

    @Test
    void invalidInitDataReturns401AndCreatesNoUser() {
        long telegramId = uniqueTelegramId();
        String tampered = sign(telegramId, "Ali", "Valiyev", "alivaliyev", "uz").replace("Valiyev", "Mallayev");

        ResponseEntity<ProblemBody> response = restTemplate.postForEntity(
                "/api/v1/auth/telegram", new LoginRequestBody(tampered), ProblemBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("auth.invalid_init_data");

        Integer userCount = jdbcTemplate.queryForObject(
                "select count(*) from users where telegram_id = ?", Integer.class, telegramId);
        assertThat(userCount).isZero();
    }

    private String sign(long telegramId, String firstName, String lastName, String username, String languageCode) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("auth_date", String.valueOf(Instant.now().getEpochSecond()));
        fields.put("query_id", "AAHtestquery");
        fields.put("user", TelegramInitDataTestSupport.userJson(telegramId, firstName, lastName, username, languageCode));
        return TelegramInitDataTestSupport.sign(fields, botToken);
    }

    private long uniqueTelegramId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, 999_999_999);
    }

    private record LoginRequestBody(String initData) {
    }

    private record UserBody(UUID id, String firstName, String language, boolean isNew) {
    }

    private record LoginResponseBody(String accessToken, String tokenType, long expiresIn, UserBody user) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProblemBody(String errorCode) {
    }

}
