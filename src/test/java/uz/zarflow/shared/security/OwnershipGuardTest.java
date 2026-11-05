package uz.zarflow.shared.security;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import uz.zarflow.shared.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipGuardTest {

    @Test
    void emptyOptionalThrowsNotFound() {
        assertThatThrownBy(() -> OwnershipGuard.requireFound(Optional.empty(), "Account"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void presentOptionalReturnsValue() {
        UUID id = UUID.randomUUID();

        UUID result = OwnershipGuard.requireFound(Optional.of(id), "Account");

        assertThat(result).isEqualTo(id);
    }

}
