package uz.zarflow.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramBotPropertiesTest {

    @Test
    void rejectsBlankBotToken() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "", "valid-secret-123", "https://example.test/webhook", "https://example.test/app", false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWebhookSecretWithInvalidCharacters() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "bad secret!", "https://example.test/webhook", "https://example.test/app", false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonHttpsMiniAppUrl() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "valid-secret-123", "https://example.test/webhook", "http://example.test/app", false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void webhookUrlCanBeBlankWhenRegisterWebhookIsFalse() {
        TelegramBotProperties properties = new TelegramBotProperties(
                "token", "valid-secret-123", "", "https://example.test/app", false);

        assertThat(properties.registerWebhook()).isFalse();
    }

    @Test
    void rejectsNonHttpsWebhookUrlWhenRegisterWebhookIsTrue() {
        assertThatThrownBy(() -> new TelegramBotProperties(
                "token", "valid-secret-123", "http://not-https.example", "https://example.test/app", true))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsValidConfiguration() {
        TelegramBotProperties properties = new TelegramBotProperties(
                "token", "valid-secret-123", "https://example.test/webhook", "https://example.test/app", true);

        assertThat(properties.botToken()).isEqualTo("token");
        assertThat(properties.registerWebhook()).isTrue();
    }

}
