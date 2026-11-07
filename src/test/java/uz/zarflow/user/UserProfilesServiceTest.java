package uz.zarflow.user;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import uz.zarflow.account.Accounts;
import uz.zarflow.shared.config.ZarflowProperties;
import uz.zarflow.shared.exception.ConflictException;
import uz.zarflow.shared.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfilesServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Accounts accounts;

    private final ZarflowProperties zarflowProperties = new ZarflowProperties("Asia/Tashkent");
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private UserProfilesService service;

    @BeforeEach
    void setUp() {
        service = new UserProfilesService(userRepository, zarflowProperties, accounts, clock);
    }

    @Test
    void zoneOfFallsBackToDefaultTimezoneWhenStoredValueIsUnparseable() {
        UUID userId = UUID.randomUUID();
        User user = new User(1L, "Ali", null, null, "uz", "Not/AValidZone", null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        assertThat(service.zoneOf(userId)).isEqualTo(ZoneId.of("Asia/Tashkent"));
    }

    @Test
    void zoneOfFallsBackToDefaultTimezoneWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThat(service.zoneOf(userId)).isEqualTo(ZoneId.of("Asia/Tashkent"));
    }

    @Test
    void zoneOfUsesStoredTimezoneWhenValid() {
        UUID userId = UUID.randomUUID();
        User user = new User(1L, "Ali", null, null, "uz", "Europe/Moscow", null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        assertThat(service.zoneOf(userId)).isEqualTo(ZoneId.of("Europe/Moscow"));
    }

    @Test
    void getProfileThrowsNotFoundForMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(userId))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getErrorCode())
                .isEqualTo("user.not_found");
    }

    @Test
    void updateSettingsTranslatesStaleVersionIntoConflictException() {
        UUID userId = UUID.randomUUID();
        User user = new User(1L, "Ali", null, null, "uz", "Asia/Tashkent", null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(User.class, userId));

        assertThatThrownBy(() -> service.updateSettings(
                userId, new UserProfiles.SettingsUpdate("ru", "Asia/Tashkent", null)))
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ConflictException) ex).getErrorCode())
                .isEqualTo("user.concurrent_update");
    }

}
