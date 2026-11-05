package uz.zarflow.shared.common;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatterTest {

    private static final Locale UZ = Locale.of("uz");
    private static final Locale RU = Locale.of("ru");

    private final MoneyFormatter formatter = new MoneyFormatter();

    @Test
    void formatsWholeAmountWithoutKopecksInUzbek() {
        String formatted = formatter.format(Money.ofWhole(8_500_000), UZ);

        assertThat(formatted).isEqualTo("8 500 000 so'm");
    }

    @Test
    void formatsWholeAmountInRussian() {
        String formatted = formatter.format(Money.ofWhole(8_500_000), RU);

        assertThat(formatted).isEqualTo("8 500 000 сум");
    }

    @Test
    void showsKopecksWhenNonZero() {
        String formatted = formatter.format(Money.of("8500000.50"), UZ);

        assertThat(formatted).isEqualTo("8 500 000,50 so'm");
    }

    @Test
    void shortFormatUsesMillionTier() {
        assertThat(formatter.formatShort(Money.ofWhole(8_500_000), UZ)).isEqualTo("8,5 mln");
        assertThat(formatter.formatShort(Money.ofWhole(8_500_000), RU)).isEqualTo("8,5 млн");
    }

    @Test
    void shortFormatBelowMillionFallsBackToFullFormat() {
        assertThat(formatter.formatShort(Money.ofWhole(500_000), UZ)).isEqualTo("500 000 so'm");
    }

}
