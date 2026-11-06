package uz.zarflow.telegram;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramBotPropertiesTest {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);

    @Test
    void rejectsBlankBotToken() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "", "valid-secret-123", "https://example.test/webhook", "https://example.test/app", false,
                CONNECT_TIMEOUT, CALL_TIMEOUT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWebhookSecretWithInvalidCharacters() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "bad secret!", "https://example.test/webhook", "https://example.test/app", false,
                CONNECT_TIMEOUT, CALL_TIMEOUT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonHttpsMiniAppUrl() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "valid-secret-123", "https://example.test/webhook", "http://example.test/app", false,
                CONNECT_TIMEOUT, CALL_TIMEOUT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void webhookUrlCanBeBlankWhenRegisterWebhookIsFalse() {
        TelegramBotProperties properties = new TelegramBotProperties(
                "token", "valid-secret-123", "", "https://example.test/app", false,
                CONNECT_TIMEOUT, CALL_TIMEOUT);

        assertThat(properties.registerWebhook()).isFalse();
    }

    @Test
    void rejectsNonHttpsWebhookUrlWhenRegisterWebhookIsTrue() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "valid-secret-123", "http://not-https.example", "https://example.test/app", true,
                CONNECT_TIMEOUT, CALL_TIMEOUT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsZeroOrNegativeConnectTimeout() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "valid-secret-123", "https://example.test/webhook", "https://example.test/app", false,
                Duration.ZERO, CALL_TIMEOUT))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "valid-secret-123", "https://example.test/webhook", "https://example.test/app", false,
                Duration.ofSeconds(-1), CALL_TIMEOUT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsZeroOrNegativeCallTimeout() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "valid-secret-123", "https://example.test/webhook", "https://example.test/app", false,
                CONNECT_TIMEOUT, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsValidConfiguration() {
        TelegramBotProperties properties = new TelegramBotProperties(
                "token", "valid-secret-123", "https://example.test/webhook", "https://example.test/app", true,
                CONNECT_TIMEOUT, CALL_TIMEOUT);

        assertThat(properties.botToken()).isEqualTo("token");
        assertThat(properties.registerWebhook()).isTrue();
        assertThat(properties.connectTimeout()).isEqualTo(CONNECT_TIMEOUT);
        assertThat(properties.callTimeout()).isEqualTo(CALL_TIMEOUT);
    }

}
