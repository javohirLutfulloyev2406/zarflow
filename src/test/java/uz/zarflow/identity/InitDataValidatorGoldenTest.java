package uz.zarflow.identity;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads a REAL initData sample captured from an actual Telegram client, so it only
 * runs where those secrets are provided (never in CI without them). If this fails,
 * stop and report — it means our reading of Telegram's algorithm is wrong, not that
 * the test needs adjusting.
 */
@EnabledIfEnvironmentVariable(named = "ZARFLOW_TEST_INIT_DATA", matches = ".+")
@EnabledIfEnvironmentVariable(named = "TELEGRAM_BOT_TOKEN", matches = ".+")
class InitDataValidatorGoldenTest {

    @Test
    void validatesARealTelegramSample() {
        String rawInitData = System.getenv("ZARFLOW_TEST_INIT_DATA");
        String botToken = System.getenv("TELEGRAM_BOT_TOKEN");

        Instant sampleAuthDate = extractAuthDate(rawInitData);
        Clock clock = Clock.fixed(sampleAuthDate, ZoneOffset.UTC);
        AuthProperties authProperties = new AuthProperties(botToken, Duration.ofHours(24), Duration.ofHours(1));
        InitDataValidator validator = new InitDataValidator(authProperties, clock);

        InitDataValidation result = validator.validate(rawInitData);

        assertThat(result).isInstanceOf(InitDataValidation.Success.class);
    }

    /** Pulled directly from the raw string, independent of InitDataValidator, just to pin the Clock. */
    private Instant extractAuthDate(String rawInitData) {
        for (String pair : rawInitData.split("&")) {
            if (pair.startsWith("auth_date=")) {
                String value = URLDecoder.decode(pair.substring("auth_date=".length()), StandardCharsets.UTF_8);
                return Instant.ofEpochSecond(Long.parseLong(value));
            }
        }
        throw new IllegalStateException("ZARFLOW_TEST_INIT_DATA has no auth_date field");
    }

}
