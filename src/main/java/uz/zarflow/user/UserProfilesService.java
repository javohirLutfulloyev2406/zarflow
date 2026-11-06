package uz.zarflow.user;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.zarflow.shared.common.Money;
import uz.zarflow.shared.config.ZarflowProperties;
import uz.zarflow.shared.exception.ConflictException;
import uz.zarflow.shared.exception.NotFoundException;

@Service
class UserProfilesService implements UserProfiles {

    private final UserRepository userRepository;
    private final ZarflowProperties zarflowProperties;

    UserProfilesService(UserRepository userRepository, ZarflowProperties zarflowProperties) {
        this.userRepository = userRepository;
        this.zarflowProperties = zarflowProperties;
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
    @Transactional(readOnly = true)
    public ZoneId zoneOf(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(User::getTimezone)
                .flatMap(this::parseZone)
                .orElseGet(() -> parseZone(zarflowProperties.defaultTimezone())
                        .orElse(ZoneId.of("UTC")));
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
                user.getLanguage(),
                user.getTimezone(),
                user.getExpectedMonthlyIncome(),
                user.getOnboardingCompletedAt() != null,
                user.getCreatedAt());
    }

    private UserSummary toSummary(User user) {
        BigDecimal income = user.getExpectedMonthlyIncome();
        ZoneId zone = parseZone(user.getTimezone())
                .orElseGet(() -> parseZone(zarflowProperties.defaultTimezone()).orElse(ZoneId.of("UTC")));
        return new UserSummary(
                user.getId(),
                user.getLanguage(),
                zone,
                income == null ? null : Money.of(income));
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
