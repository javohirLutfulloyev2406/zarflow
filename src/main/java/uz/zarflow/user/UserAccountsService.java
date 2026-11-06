package uz.zarflow.user;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.shared.config.ZarflowProperties;
import uz.zarflow.shared.exception.ConflictException;

@Service
class UserAccountsService implements UserAccounts {

    private final UserRepository userRepository;
    private final ZarflowProperties zarflowProperties;

    UserAccountsService(UserRepository userRepository, ZarflowProperties zarflowProperties) {
        this.userRepository = userRepository;
        this.zarflowProperties = zarflowProperties;
    }

    @Override
    @Transactional
    public LoginResult registerOrUpdate(TelegramProfile profile) {
        return userRepository.findByTelegramId(profile.telegramId())
                .map(existing -> updateExisting(existing, profile))
                .orElseGet(() -> createNew(profile));
    }

    private LoginResult updateExisting(User user, TelegramProfile profile) {
        user.updateProfile(profile.firstName(), profile.lastName(), profile.username());
        try {
            userRepository.saveAndFlush(user);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("user.concurrent_update");
        }
        return new LoginResult(user.getId(), user.getFirstName(), user.getLanguage(), false);
    }

    private LoginResult createNew(TelegramProfile profile) {
        String language = defaultLanguageFor(profile.languageCode());
        User user = new User(
                profile.telegramId(),
                profile.firstName(),
                profile.lastName(),
                profile.username(),
                language,
                zarflowProperties.defaultTimezone(),
                null);
        userRepository.save(user);
        return new LoginResult(user.getId(), user.getFirstName(), user.getLanguage(), true);
    }

    private String defaultLanguageFor(String languageCode) {
        return languageCode != null && languageCode.startsWith("ru") ? "ru" : "uz";
    }

}
