package uz.zarflow.humo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HumoFragmentDetectorTest {

    @Test
    void flagsEachLineOfASplitTransactionNotification() {
        assertThat(HumoFragmentDetector.looksLikeFragment("💸 Оплата")).isTrue();
        assertThat(HumoFragmentDetector.looksLikeFragment("➖ 47.000,00 UZS")).isTrue();
        assertThat(HumoFragmentDetector.looksLikeFragment("📍 ZIYODA BARAKA BIZNES")).isTrue();
        assertThat(HumoFragmentDetector.looksLikeFragment("💳 HUMOCARD *8643")).isTrue();
        assertThat(HumoFragmentDetector.looksLikeFragment("🕓 12:00 12.11.2026")).isTrue();
    }

    @Test
    void flagsBalanceMarkersToo() {
        assertThat(HumoFragmentDetector.looksLikeFragment("🔹 HUMOCARD *1234")).isTrue();
        assertThat(HumoFragmentDetector.looksLikeFragment("💵 109'151.58")).isTrue();
    }

    @Test
    void doesNotFlagOrdinaryQuickInputText() {
        assertThat(HumoFragmentDetector.looksLikeFragment("5000 taxi")).isFalse();
        assertThat(HumoFragmentDetector.looksLikeFragment("Taxi uchun 5000 ketdi")).isFalse();
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(HumoFragmentDetector.looksLikeFragment(null)).isFalse();
        assertThat(HumoFragmentDetector.looksLikeFragment("")).isFalse();
    }

}
