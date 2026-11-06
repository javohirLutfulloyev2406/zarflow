package uz.zarflow.telegram;

/**
 * Deliberately never carries the original {@code TelegramApiException} as its cause —
 * that exception's message can embed the full request URL, which contains the bot token.
 */
class TelegramGatewayException extends RuntimeException {

    TelegramGatewayException(String telegramMethod) {
        super("Telegram API call failed: " + telegramMethod);
    }

}
