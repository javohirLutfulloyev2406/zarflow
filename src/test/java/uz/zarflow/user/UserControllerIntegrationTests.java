package uz.zarflow.user;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private Clock clock;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getMeReturnsOwnProfileWithMoneyAsStringAndNoTelegramId() {
        UUID userId = createUser("Ali", "uz", "Asia/Tashkent", "8500000.00");

        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.get("/api/v1/users/me").header("Authorization", "Bearer " + token(userId)).build(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"expectedMonthlyIncome\":\"8500000.00\"");
        assertThat(response.getBody()).doesNotContain("telegramId");
    }

    @Test
    void getMeWithoutTokenIsUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMeForSoftDeletedUserReturnsNotFound() {
        UUID userId = createUser("Ali", "uz", "Asia/Tashkent", null);
        jdbcTemplate.update("update users set deleted_at = now() where id = ?", userId);

        ResponseEntity<ProblemBody> response = restTemplate.exchange(
                RequestEntity.get("/api/v1/users/me").header("Authorization", "Bearer " + token(userId)).build(),
                ProblemBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errorCode()).isEqualTo("user.not_found");
    }

    @Test
    void putSettingsPersistsValuesAndIncrementsVersion() {
        UUID userId = createUser("Ali", "uz", "Asia/Tashkent", null);
        Long versionBefore = jdbcTemplate.queryForObject("select version from users where id = ?", Long.class, userId);
        Instant updatedAtBefore = jdbcTemplate.queryForObject(
                "select updated_at from users where id = ?", java.sql.Timestamp.class, userId).toInstant();

        ResponseEntity<String> response = restTemplate.exchange(
                RequestEntity.put("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + token(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"language\":\"ru\",\"timezone\":\"Europe/Moscow\",\"expectedMonthlyIncome\":\"1234567.89\"}"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String language = jdbcTemplate.queryForObject("select language from users where id = ?", String.class, userId);
        String timezone = jdbcTemplate.queryForObject("select timezone from users where id = ?", String.class, userId);
        BigDecimal income = jdbcTemplate.queryForObject(
                "select expected_monthly_income from users where id = ?", BigDecimal.class, userId);
        Long versionAfter = jdbcTemplate.queryForObject("select version from users where id = ?", Long.class, userId);
        Instant updatedAtAfter = jdbcTemplate.queryForObject(
                "select updated_at from users where id = ?", java.sql.Timestamp.class, userId).toInstant();

        assertThat(language).isEqualTo("ru");
        assertThat(timezone).isEqualTo("Europe/Moscow");
        assertThat(income).isEqualByComparingTo("1234567.89");
        assertThat(versionAfter).isEqualTo(versionBefore + 1);
        assertThat(updatedAtAfter).isAfter(updatedAtBefore);
    }

    @Test
    void putSettingsByUserANeverChangesUserB() {
        UUID userA = createUser("Ali", "uz", "Asia/Tashkent", null);
        UUID userB = createUser("Vali", "uz", "Asia/Tashkent", null);

        restTemplate.exchange(
                RequestEntity.put("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + token(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"language\":\"ru\",\"timezone\":\"Europe/Moscow\",\"expectedMonthlyIncome\":null}"),
                String.class);

        String languageB = jdbcTemplate.queryForObject("select language from users where id = ?", String.class, userB);
        String timezoneB = jdbcTemplate.queryForObject("select timezone from users where id = ?", String.class, userB);

        assertThat(languageB).isEqualTo("uz");
        assertThat(timezoneB).isEqualTo("Asia/Tashkent");
    }

    @Test
    void invalidLanguageReturns400() {
        assertValidationFails(createUser("Ali", "uz", "Asia/Tashkent", null),
                "{\"language\":\"en\",\"timezone\":\"Asia/Tashkent\"}");
    }

    @Test
    void offsetStyleTimezoneReturns400() {
        assertValidationFails(createUser("Ali", "uz", "Asia/Tashkent", null),
                "{\"language\":\"uz\",\"timezone\":\"UTC+5\"}");
    }

    @Test
    void nonExistentTimezoneReturns400() {
        assertValidationFails(createUser("Ali", "uz", "Asia/Tashkent", null),
                "{\"language\":\"uz\",\"timezone\":\"Mars/Base\"}");
    }

    @Test
    void negativeIncomeReturns400() {
        assertValidationFails(createUser("Ali", "uz", "Asia/Tashkent", null),
                "{\"language\":\"uz\",\"timezone\":\"Asia/Tashkent\",\"expectedMonthlyIncome\":\"-1\"}");
    }

    @Test
    void tooManyDecimalPlacesReturns400() {
        assertValidationFails(createUser("Ali", "uz", "Asia/Tashkent", null),
                "{\"language\":\"uz\",\"timezone\":\"Asia/Tashkent\",\"expectedMonthlyIncome\":\"1.234\"}");
    }

    @Test
    void incomeAsJsonNumberReturns400() {
        assertValidationFails(createUser("Ali", "uz", "Asia/Tashkent", null),
                "{\"language\":\"uz\",\"timezone\":\"Asia/Tashkent\",\"expectedMonthlyIncome\":8500000}");
    }

    @Test
    void missingLanguageReturns400() {
        assertValidationFails(createUser("Ali", "uz", "Asia/Tashkent", null),
                "{\"timezone\":\"Asia/Tashkent\"}");
    }

    @Test
    void validationErrorDetailIsLocalizedForRussianAcceptLanguage() {
        UUID userId = createUser("Ali", "uz", "Asia/Tashkent", null);

        ResponseEntity<ProblemBody> response = restTemplate.exchange(
                RequestEntity.put("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + token(userId))
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"language\":\"en\",\"timezone\":\"Asia/Tashkent\"}"),
                ProblemBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().detail()).isEqualTo("В введённых данных есть ошибка.");
    }

    @Test
    void staleVersionOnSaveIsTranslatedToOptimisticLockFailure() {
        UUID userId = createUser("Ali", "uz", "Asia/Tashkent", null);

        User loaded = transactionTemplate.execute(
                status -> userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow());
        jdbcTemplate.update("update users set version = version + 1 where id = ?", userId);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            loaded.updateSettings("ru", "Asia/Tashkent", null);
            userRepository.saveAndFlush(loaded);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private void assertValidationFails(UUID userId, String body) {
        ResponseEntity<ProblemBody> response = restTemplate.exchange(
                RequestEntity.put("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer " + token(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body),
                ProblemBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("validation-error");
    }

    private UUID createUser(String firstName, String language, String timezone, String expectedMonthlyIncome) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, telegram_id, first_name, language, timezone, expected_monthly_income) "
                        + "values (?, ?, ?, ?, ?, ?)",
                id, uniqueTelegramId(), firstName, language, timezone,
                expectedMonthlyIncome == null ? null : new BigDecimal(expectedMonthlyIncome));
        return id;
    }

    private long uniqueTelegramId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, 999_999_999);
    }

    private String token(UUID userId) {
        Instant now = Instant.now(clock);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("zarflow")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProblemBody(String errorCode, String detail) {
    }

}
