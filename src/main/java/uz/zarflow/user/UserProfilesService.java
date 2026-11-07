package uz.zarflow.user;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.account.Accounts;
import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.config.ZarflowProperties;
import uz.zarflow.shared.exception.BusinessRuleException;
import uz.zarflow.shared.exception.ConflictException;
import uz.zarflow.shared.exception.NotFoundException;

@Service
class UserProfilesService implements UserProfiles {

    private final UserRepository userRepository;
    private final ZarflowProperties zarflowProperties;
    private final Accounts accounts;
    private final Clock clock;

    UserProfilesService(UserRepository userRepository, ZarflowProperties zarflowProperties, Accounts accounts, Clock clock) {
        this.userRepository = userRepository;
        this.zarflowProperties = zarflowProperties;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID userId) {
        return toProfile(requireActiveUser(userId));
    }

    @Override
    @Transactional
    public UserProfile updateSettings(UUID userId, SettingsUpdate update) {
        User user = requireActiveUser(userId);
        user.updateSettings(update.language(), update.timezone(), update.expectedMonthlyIncome());
        try {
            userRepository.saveAndFlush(user);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("user.concurrent_update");
        }
        return toProfile(user);
    }

    @Override
    @Transactional
    public UserProfile completeOnboarding(UUID userId, OnboardingInput input) {
        User user = requireActiveUser(userId);
        if (user.getOnboardingCompletedAt() != null) {
            throw new BusinessRuleException("user.onboarding_already_completed");
        }
        accounts.create(userId, new Accounts.CreateAccount(
                input.accountName(), input.accountType(), input.initialBalance(), null, true));
        user.completeOnboarding(input.expectedMonthlyIncome(), Instant.now(clock));
        try {
            userRepository.saveAndFlush(user);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("user.concurrent_update");
        }
        return toProfile(user);
    }

    @Override
    @Transactional
    public UserProfile updateDisplayName(UUID userId, String displayName) {
        User user = requireActiveUser(userId);
        user.updateDisplayName(displayName.trim());
        try {
            userRepository.saveAndFlush(user);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("user.concurrent_update");
        }
        return toProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneId zoneOf(UUID userId) {
        return resolveZone(userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(User::getTimezone)
                .orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> find(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId).map(this::toSummary);
    }

    private User requireActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("user.not_found"));
    }

    private UserProfile toProfile(User user) {
        return new UserProfile(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getDisplayName(),
                user.getLanguage(),
                user.getTimezone(),
                user.getExpectedMonthlyIncome(),
                user.getOnboardingCompletedAt() != null,
                user.getCreatedAt());
    }

    private UserSummary toSummary(User user) {
        BigDecimal income = user.getExpectedMonthlyIncome();
        return new UserSummary(
                user.getId(),
                user.getLanguage(),
                resolveZone(user.getTimezone()),
                income == null ? null : Money.of(income));
    }

    private ZoneId resolveZone(String zoneId) {
        return parseZone(zoneId)
                .orElseGet(() -> parseZone(zarflowProperties.defaultTimezone()).orElse(ZoneId.of("UTC")));
    }

    private Optional<ZoneId> parseZone(String zoneId) {
        if (zoneId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZoneId.of(zoneId));
        } catch (DateTimeException ex) {
            return Optional.empty();
        }
    }

}
