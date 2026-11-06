package uz.zarflow.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Secret-token and body-size checks happen earlier, in {@link TelegramWebhookGuardFilter}
 * (registered only for {@link #WEBHOOK_PATH}) -- by the time a request reaches here, both
 * have already passed.
 */
@RestController
class TelegramWebhookController {

    /** Single source of truth for this route — TelegramBotStartupRunner reuses it when registering the webhook. */
    static final String WEBHOOK_PATH = "/telegram/webhook";

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final ObjectMapper objectMapper;
    private final TelegramUpdateService updateService;

    TelegramWebhookController(ObjectMapper objectMapper, TelegramUpdateService updateService) {
        this.objectMapper = objectMapper;
        this.updateService = updateService;
    }

    @PostMapping(WEBHOOK_PATH)
    ResponseEntity<Void> webhook(@RequestBody byte[] body) {
        try {
            Update update = objectMapper.readValue(body, Update.class);
            updateService.handle(update);
        } catch (Exception ex) {
            log.error("Webhook processing failed, requestId={}", MDC.get("requestId"), ex);
        }
        return ResponseEntity.ok().build();
    }

}
