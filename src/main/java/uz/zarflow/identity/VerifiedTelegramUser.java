package uz.zarflow.identity;

import java.time.Instant;

record VerifiedTelegramUser(
        long telegramId,
        String firstName,
        String lastName,
        String username,
        String languageCode,
        Instant authDate) {
}
