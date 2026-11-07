package uz.zarflow.shared.common;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonthNamesTest {

    @Test
    void uzbekUsesDayDashMonth() {
        String label = MonthNames.dayAndMonth(LocalDate.of(2026, 11, 7), Locale.of("uz"));

        assertThat(label).isEqualTo("7-noyabr");
    }

    @Test
    void russianUsesDaySpaceMonthGenitive() {
        String label = MonthNames.dayAndMonth(LocalDate.of(2026, 11, 7), Locale.of("ru"));

        assertThat(label).isEqualTo("7 ноября");
    }

    @Test
    void handlesJanuaryBoundary() {
        String label = MonthNames.dayAndMonth(LocalDate.of(2027, 1, 1), Locale.of("uz"));

        assertThat(label).isEqualTo("1-yanvar");
    }

}
