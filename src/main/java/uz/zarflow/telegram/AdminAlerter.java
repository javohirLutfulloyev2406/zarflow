package uz.zarflow.telegram;

/**
 * Operational error alerting to the admin's Telegram chat ({@code zarflow.telegram.admin-chat-id}).
 * Other modules reach Telegram for this only through here, never through {@code TelegramGateway}
 * directly -- same rule as {@link BotNotifications}. A no-op (silently does nothing) when no
 * admin chat id is configured.
 *
 * <p>Callers must never pass exception messages or context strings that could carry request
 * bodies, tokens, Telegram initData, amounts, or user names -- a user id (UUID) is the most
 * identifying detail that's ever safe to include.
 */
public interface AdminAlerter {

    /** {@code source} is a short label like "endpoint GET /api/v1/transactions", "job:dailyReport",
     * or "webhook" -- it ends up in the alert text, so keep it free of any of the data above. */
    void alert(String source, Throwable ex);

}
