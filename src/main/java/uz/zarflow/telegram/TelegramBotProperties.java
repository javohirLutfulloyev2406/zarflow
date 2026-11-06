package uz.zarflow.telegram;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "zarflow.telegram")
record TelegramBotProperties(
        String botToken,
        String webhookSecret,
        String webhookUrl,
        String miniAppUrl,
        @DefaultValue("false") boolean registerWebhook) {

    private static final Pattern SECRET_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,256}");

    TelegramBotProperties {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("zarflow.telegram.bot-token must not be blank");
        }
        if (webhookSecret == null || !SECRET_PATTERN.matcher(webhookSecret).matches()) {
            throw new IllegalStateException("zarflow.telegram.webhook-secret must match [A-Za-z0-9_-]{1,256}");
        }
        requireHttps(miniAppUrl, "zarflow.telegram.mini-app-url");
        if (registerWebhook) {
            requireHttps(webhookUrl, "zarflow.telegram.webhook-url");
        }
    }

    private static void requireHttps(String url, String propertyName) {
        if (url == null || !url.startsWith("https://")) {
            throw new IllegalStateException(propertyName + " must be an https:// URL");
        }
    }

}
