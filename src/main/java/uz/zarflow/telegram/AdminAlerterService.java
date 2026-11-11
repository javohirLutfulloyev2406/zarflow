package uz.zarflow.telegram;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Sends operational alerts to one fixed admin Telegram chat, independent of any app user.
 * Disabled entirely (every call is a silent no-op) when {@code zarflow.telegram.admin-chat-id}
 * is blank -- that's the expected local/dev state, not a misconfiguration.
 */
@Service
class AdminAlerterService implements AdminAlerter {

    private static final Logger log = LoggerFactory.getLogger(AdminAlerterService.class);
    private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Duration COOLDOWN = Duration.ofMinutes(15);
    private static final int MAX_PER_HOUR = 20;

    private final TelegramGateway gateway;
    private final Clock clock;
    private final Long adminChatId;
    private final String buildSha;
    private final AlertRateLimiter rateLimiter;

    AdminAlerterService(
            TelegramGateway gateway, TelegramBotProperties properties, Clock clock,
            @Value("${zarflow.build-sha:unknown}") String buildSha) {
        this.gateway = gateway;
        this.clock = clock;
        this.adminChatId = parseChatId(properties.adminChatId());
        this.buildSha = buildSha;
        this.rateLimiter = new AlertRateLimiter(clock, COOLDOWN, MAX_PER_HOUR);
    }

    @Override
    public void alert(String source, Throwable ex) {
        if (adminChatId == null) {
            return;
        }
        String signature = source + "::" + ex.getClass().getName();
        AlertRateLimiter.Decision decision = rateLimiter.tryAcquire(signature);
        if (!decision.allowed()) {
            return;
        }

        StringBuilder text = new StringBuilder("🔴 ")
                .append(ex.getClass().getSimpleName()).append(" · ").append(source);
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            text.append(" · ").append(requestId);
        }
        text.append(" · ").append(TIME_FORMAT.format(Instant.now(clock).atZone(TASHKENT)));
        if (decision.suppressedCount() > 0) {
            text.append("\n(+").append(decision.suppressedCount()).append(" times since last alert)");
        }
        sendSafely(text.toString());
    }

    @EventListener(ApplicationReadyEvent.class)
    void announceStartup() {
        if (adminChatId == null) {
            return;
        }
        sendSafely("🟢 ZarFlow started · " + buildSha);
    }

    private void sendSafely(String text) {
        try {
            gateway.sendMessage(adminChatId, text, null);
        } catch (TelegramGatewayException ex) {
            log.warn("Admin alert delivery failed: {}", ex.getMessage());
        }
    }

    private static Long parseChatId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            log.warn("zarflow.telegram.admin-chat-id is not a valid chat id; admin alerts disabled");
            return null;
        }
    }

}
