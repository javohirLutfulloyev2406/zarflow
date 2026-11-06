package uz.zarflow.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
class TelegramWebhookController {

    /** Single source of truth for this route — TelegramBotStartupRunner reuses it when registering the webhook. */
    static final String WEBHOOK_PATH = "/telegram/webhook";

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";
    private static final int MAX_BODY_BYTES = 1_000_000;

    private final TelegramBotProperties properties;
    private final ObjectMapper objectMapper;
    private final TelegramUpdateService updateService;

    TelegramWebhookController(
            TelegramBotProperties properties, ObjectMapper objectMapper, TelegramUpdateService updateService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.updateService = updateService;
    }

    @PostMapping(WEBHOOK_PATH)
    ResponseEntity<Void> webhook(
            @RequestHeader(value = SECRET_HEADER, required = false) String secretHeader,
            @RequestBody byte[] body) {
        if (!isValidSecret(secretHeader)) {
            log.warn("Rejected webhook request: missing or invalid secret token, requestId={}", MDC.get("requestId"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (body.length > MAX_BODY_BYTES) {
            log.warn("Rejected webhook body: {} bytes exceeds limit, requestId={}", body.length, MDC.get("requestId"));
            return ResponseEntity.ok().build();
        }
        try {
            Update update = objectMapper.readValue(body, Update.class);
            updateService.handle(update);
        } catch (Exception ex) {
            log.error("Webhook processing failed, requestId={}", MDC.get("requestId"), ex);
        }
        return ResponseEntity.ok().build();
    }

    private boolean isValidSecret(String secretHeader) {
        if (secretHeader == null) {
            return false;
        }
        byte[] provided = secretHeader.getBytes(StandardCharsets.UTF_8);
        byte[] expected = properties.webhookSecret().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, expected);
    }

}
