package uz.zarflow.identity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InitDataValidatorTest {

    private static final String BOT_TOKEN = "test-bot-token-fixture";
    private static final Instant FIXED_NOW = Instant.parse("2026-11-05T12:00:00Z");

    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final AuthProperties authProperties =
            new AuthProperties(BOT_TOKEN, Duration.ofHours(24), Duration.ofHours(1));
    private final InitDataValidator validator = new InitDataValidator(authProperties, clock);

    private Map<String, String> baseFields(Instant authDate) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        fields.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        fields.put("user", TelegramInitDataTestSupport.userJson(123456789L, "Ali", "Valiyev", "alivaliyev", "uz"));
        return fields;
    }

    @Test
    void validInitDataSucceeds() {
        String raw = TelegramInitDataTestSupport.sign(baseFields(FIXED_NOW), BOT_TOKEN);

        InitDataValidation result = validator.validate(raw);

        assertThat(result).isInstanceOf(InitDataValidation.Success.class);
        VerifiedTelegramUser user = ((InitDataValidation.Success) result).user();
        assertThat(user.telegramId()).isEqualTo(123456789L);
        assertThat(user.firstName()).isEqualTo("Ali");
        assertThat(user.lastName()).isEqualTo("Valiyev");
        assertThat(user.username()).isEqualTo("alivaliyev");
        assertThat(user.languageCode()).isEqualTo("uz");
    }

    @Test
    void tamperedUserFieldFailsSignature() {
        String raw = TelegramInitDataTestSupport.sign(baseFields(FIXED_NOW), BOT_TOKEN);
        String tampered = raw.replace("Valiyev", "Mallayev");

        assertFailure(validator.validate(tampered), FailureReason.BAD_SIGNATURE);
    }

    @Test
    void tamperedAuthDateFailsSignature() {
        String raw = TelegramInitDataTestSupport.sign(baseFields(FIXED_NOW), BOT_TOKEN);
        String originalAuthDate = "auth_date=" + FIXED_NOW.getEpochSecond();
        String tamperedAuthDate = "auth_date=" + FIXED_NOW.plusSeconds(1).getEpochSecond();
        String tampered = raw.replace(originalAuthDate, tamperedAuthDate);

        assertFailure(validator.validate(tampered), FailureReason.BAD_SIGNATURE);
    }

    @Test
    void wrongBotTokenFailsSignature() {
        String raw = TelegramInitDataTestSupport.sign(baseFields(FIXED_NOW), "a-different-bot-token");

        assertFailure(validator.validate(raw), FailureReason.BAD_SIGNATURE);
    }

    @Test
    void missingHashIsRejected() {
        Map<String, String> fields = baseFields(FIXED_NOW);
        StringBuilder raw = new StringBuilder();
        fields.forEach((key, value) -> {
            if (!raw.isEmpty()) {
                raw.append('&');
            }
            raw.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });

        assertFailure(validator.validate(raw.toString()), FailureReason.MISSING_HASH);
    }

    @Test
    void duplicateKeyIsMalformed() {
        String raw = "auth_date=" + FIXED_NOW.getEpochSecond()
                + "&auth_date=" + FIXED_NOW.getEpochSecond()
                + "&hash=deadbeef";

        assertFailure(validator.validate(raw), FailureReason.MALFORMED);
    }

    @Test
    void expiredWhenClockMovedPastMaxAge() {
        Instant authDate = FIXED_NOW.minus(authProperties.initDataMaxAge()).minusSeconds(1);
        String raw = TelegramInitDataTestSupport.sign(baseFields(authDate), BOT_TOKEN);

        assertFailure(validator.validate(raw), FailureReason.EXPIRED);
    }

    @Test
    void expiredWhenAuthDateTooFarInFuture() {
        Instant authDate = FIXED_NOW.plusSeconds(61);
        String raw = TelegramInitDataTestSupport.sign(baseFields(authDate), BOT_TOKEN);

        assertFailure(validator.validate(raw), FailureReason.EXPIRED);
    }

    @Test
    void unicodeNamesRoundTripCorrectly() {
        Map<String, String> fields = baseFields(FIXED_NOW);
        fields.put("user", TelegramInitDataTestSupport.userJson(
                42L, "Алексей", "Иванов 😀", "alex_ivanov", "ru"));
        String raw = TelegramInitDataTestSupport.sign(fields, BOT_TOKEN);

        InitDataValidation result = validator.validate(raw);

        assertThat(result).isInstanceOf(InitDataValidation.Success.class);
        VerifiedTelegramUser user = ((InitDataValidation.Success) result).user();
        assertThat(user.firstName()).isEqualTo("Алексей");
        assertThat(user.lastName()).isEqualTo("Иванов 😀");
    }

    private void assertFailure(InitDataValidation result, FailureReason expected) {
        assertThat(result).isInstanceOf(InitDataValidation.Failure.class);
        assertThat(((InitDataValidation.Failure) result).reason()).isEqualTo(expected);
    }

}
