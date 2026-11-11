package uz.zarflow.humo;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HumoReconciliationMathTest {

    @Test
    void exactMatchCountsAsMatching() {
        var result = HumoReconciliationMath.classify(new BigDecimal("109151.58"), new BigDecimal("109151.58"));

        assertThat(result.matches()).isTrue();
        assertThat(result.diff()).isEqualByComparingTo("0.00");
    }

    @Test
    void diffUnderOneSomStillCountsAsMatching() {
        var result = HumoReconciliationMath.classify(new BigDecimal("109151.58"), new BigDecimal("109150.80"));

        assertThat(result.matches()).isTrue();
        assertThat(result.diff()).isEqualByComparingTo("0.78");
    }

    @Test
    void diffOfOneOrMoreIsAMismatch() {
        var result = HumoReconciliationMath.classify(new BigDecimal("109151.58"), new BigDecimal("120000.00"));

        assertThat(result.matches()).isFalse();
        assertThat(result.diff()).isEqualByComparingTo("-10848.42");
    }

    @Test
    void negativeDiffBelowThresholdStillMatches() {
        var result = HumoReconciliationMath.classify(new BigDecimal("1000.00"), new BigDecimal("1000.99"));

        assertThat(result.matches()).isTrue();
        assertThat(result.diff()).isEqualByComparingTo("-0.99");
    }

}
