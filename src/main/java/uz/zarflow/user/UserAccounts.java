package uz.zarflow.user;

import java.util.UUID;

/**
 * The user module's public API. Other modules (identity's login flow) talk to
 * users only through here — never through {@code User}/{@code UserRepository}.
 */
public interface UserAccounts {

    LoginResult registerOrUpdate(TelegramProfile profile);

    record TelegramProfile(
            long telegramId,
            String firstName,
            String lastName,
            String username,
            String languageCode) {
    }

    record LoginResult(UUID userId, String firstName, String displayName, String language, boolean isNew) {
    }

}
