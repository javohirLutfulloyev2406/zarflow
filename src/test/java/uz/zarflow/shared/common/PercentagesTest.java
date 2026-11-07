package uz.zarflow.shared.common;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PercentagesTest {

    @Test
    void changeOfIsPositiveWhenCurrentIsHigher() {
        BigDecimal change = Percentages.changeOf(new BigDecimal("150000"), new BigDecimal("100000"));

        assertThat(change).isEqualByComparingTo("50.0");
    }

    @Test
    void changeOfIsNegativeWhenCurrentIsLower() {
        BigDecimal change = Percentages.changeOf(new BigDecimal("80000"), new BigDecimal("100000"));

        assertThat(change).isEqualByComparingTo("-20.0");
    }

    @Test
    void changeOfIsNullWhenPreviousIsZero() {
        BigDecimal change = Percentages.changeOf(new BigDecimal("50000"), BigDecimal.ZERO);

        assertThat(change).isNull();
    }

    @Test
    void changeOfIsNullWhenPreviousIsNull() {
        BigDecimal change = Percentages.changeOf(new BigDecimal("50000"), null);

        assertThat(change).isNull();
    }

    @Test
    void shareOfComputesPercentOfTotal() {
        BigDecimal share = Percentages.shareOf(new BigDecimal("25000"), new BigDecimal("100000"));

        assertThat(share).isEqualByComparingTo("25.0");
    }

    @Test
    void shareOfIsZeroWhenTotalIsZero() {
        BigDecimal share = Percentages.shareOf(new BigDecimal("25000"), BigDecimal.ZERO);

        assertThat(share).isEqualByComparingTo("0.0");
    }

}
