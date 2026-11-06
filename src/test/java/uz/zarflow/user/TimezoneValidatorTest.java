package uz.zarflow.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimezoneValidatorTest {

    private final TimezoneValidator validator = new TimezoneValidator();

    @Test
    void acceptsRegionBasedIanaId() {
        assertThat(validator.isValid("Asia/Tashkent", null)).isTrue();
    }

    @Test
    void acceptsNullAsNotThisValidatorsConcern() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void rejectsFixedOffsetStrings() {
        assertThat(validator.isValid("+05:00", null)).isFalse();
    }

    @Test
    void rejectsBareZ() {
        assertThat(validator.isValid("Z", null)).isFalse();
    }

    @Test
    void rejectsOffsetStyleUtcPlusFive() {
        assertThat(validator.isValid("UTC+5", null)).isFalse();
    }

    @Test
    void rejectsUnknownRegion() {
        assertThat(validator.isValid("Mars/Base", null)).isFalse();
    }

}
