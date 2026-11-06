package uz.zarflow.telegram;

import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "zarflow.telegram")
record TelegramBotProperties(
        String botToken,
        String webhookSecret,
        String webhookUrl,
        String miniAppUrl,
        @DefaultValue("false") boolean registerWebhook,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("10s") Duration callTimeout) {

    private static final Pattern SECRET_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,256}");

    TelegramBotProperties {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("zarflow.telegram.bot-token must not be blank");
        }
        if (webhookSecret == null || !SECRET_PATTERN.matcher(webhookSecret).matches()) {
            throw new IllegalStateException("zarflow.telegram.webhook-secret must match [A-Za-z0-9_-]{1,256}");
        }
        requireHttps(miniAppUrl, "zarflow.telegram.mini-app-url");
        // webhookUrl is only meaningful when this instance actually registers the webhook
        // on startup (TelegramBotStartupRunner); other instances (e.g. local dev with
        // register-webhook=false) never read it, so it's fine left blank/invalid for them.
        if (registerWebhook) {
            requireHttps(webhookUrl, "zarflow.telegram.webhook-url");
        }
        requirePositive(connectTimeout, "zarflow.telegram.connect-timeout");
        requirePositive(callTimeout, "zarflow.telegram.call-timeout");
    }

    private static void requireHttps(String url, String propertyName) {
        if (url == null || !url.startsWith("https://")) {
            throw new IllegalStateException(propertyName + " must be an https:// URL");
        }
    }

    private static void requirePositive(Duration duration, String propertyName) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalStateException(propertyName + " must be a positive duration");
        }
    }

}
