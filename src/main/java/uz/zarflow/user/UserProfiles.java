package uz.zarflow.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import uz.zarflow.account.AccountType;
import uz.zarflow.shared.common.Money;

/**
 * The user module's public API for profile/settings data. Other modules talk to
 * users only through here — never through {@code User}/{@code UserRepository}.
 */
public interface UserProfiles {

    UserProfile getProfile(UUID userId);

    UserProfile updateSettings(UUID userId, SettingsUpdate update);

    UserProfile completeOnboarding(UUID userId, OnboardingInput input);

    ZoneId zoneOf(UUID userId);

    Optional<UserSummary> find(UUID userId);

    record UserProfile(
            UUID id,
            String firstName,
            String lastName,
            String username,
            String language,
            String timezone,
            BigDecimal expectedMonthlyIncome,
            boolean onboardingCompleted,
            Instant createdAt) {
    }

    record SettingsUpdate(String language, String timezone, BigDecimal expectedMonthlyIncome) {
    }

    record OnboardingInput(
            String accountName, AccountType accountType, BigDecimal initialBalance, BigDecimal expectedMonthlyIncome) {
    }

    record UserSummary(UUID id, String language, ZoneId timezone, Money expectedMonthlyIncome) {
    }

}
