package uz.zarflow.shared.common;

/**
 * Fire-and-forget request for an admin Telegram alert, published via
 * {@code ApplicationEventPublisher}. Exists so callers (exception handling, scheduled
 * jobs, webhook processing -- spread across several modules) never need a direct
 * dependency on the telegram module to report a failure; {@code AdminAlerterService}
 * is the sole listener. A plain method-call seam here would risk circular module
 * dependencies, since telegram itself depends on other modules (e.g. budget).
 *
 * <p>{@code source} ends up verbatim in the alert text -- never a request body, token,
 * Telegram initData, amount, or user name. A user id (UUID) is the most identifying
 * detail that's ever safe to include.
 */
public record AdminAlertRequested(String source, Throwable exception) {
}
