package uz.zarflow.user;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import uz.zarflow.shared.config.ZarflowProperties;
import uz.zarflow.shared.exception.ConflictException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountsServiceTest {

    @Mock
    private UserRepository userRepository;

    private final ZarflowProperties zarflowProperties = new ZarflowProperties("Asia/Tashkent");
    private UserAccountsService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountsService(userRepository, zarflowProperties);
    }

    @Test
    void registerOrUpdateTranslatesStaleVersionOnLoginUpsertIntoConflictException() {
        long telegramId = 123456789L;
        User existing = new User(telegramId, "Ali", null, null, "uz", "Asia/Tashkent", null);
        when(userRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(existing));
        when(userRepository.saveAndFlush(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(User.class, telegramId));

        UserAccounts.TelegramProfile profile =
                new UserAccounts.TelegramProfile(telegramId, "Alisher", null, null, "uz");

        assertThatThrownBy(() -> service.registerOrUpdate(profile))
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ConflictException) ex).getErrorCode())
                .isEqualTo("user.concurrent_update");
    }

}
