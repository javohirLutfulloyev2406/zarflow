package uz.zarflow.telegram;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hand-builds raw Telegram webhook JSON payloads for tests — deliberately not reusing
 * any production deserialization code, so a shared bug wouldn't be invisible to tests.
 */
final class TelegramUpdateTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private TelegramUpdateTestSupport() {
    }

    static String privateMessageUpdate(
            long updateId, long chatId, long telegramUserId, String firstName, String lastName,
            String username, String languageCode, String text) {
        return messageUpdate(updateId, chatId, "private", telegramUserId, firstName, lastName, username,
                languageCode, false, text);
    }

    static String groupMessageUpdate(long updateId, long chatId, long telegramUserId, String text) {
        return messageUpdate(updateId, chatId, "group", telegramUserId, "Ali", null, null, "uz", false, text);
    }

    static String botSenderMessageUpdate(long updateId, long chatId, long telegramUserId, String text) {
        return messageUpdate(updateId, chatId, "private", telegramUserId, "SomeBot", null, null, "uz", true, text);
    }

    private static String messageUpdate(
            long updateId, long chatId, String chatType, long telegramUserId, String firstName, String lastName,
            String username, String languageCode, boolean isBot, String text) {
        try {
            Map<String, Object> from = new LinkedHashMap<>();
            from.put("id", telegramUserId);
            from.put("is_bot", isBot);
            from.put("first_name", firstName);
            from.put("last_name", lastName);
            from.put("username", username);
            from.put("language_code", languageCode);

            Map<String, Object> chat = new LinkedHashMap<>();
            chat.put("id", chatId);
            chat.put("type", chatType);

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("message_id", 1);
            message.put("date", Instant.now().getEpochSecond());
            message.put("chat", chat);
            message.put("from", from);
            message.put("text", text);

            Map<String, Object> update = new LinkedHashMap<>();
            update.put("update_id", updateId);
            update.put("message", message);
            return JSON.writeValueAsString(update);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
