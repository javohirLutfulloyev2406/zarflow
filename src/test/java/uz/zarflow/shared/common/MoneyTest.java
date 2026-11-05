package uz.zarflow.shared.common;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void scaleIsAlwaysTwo() {
        assertThat(Money.of("5").amount().scale()).isEqualTo(2);
        assertThat(Money.ofWhole(5).amount().scale()).isEqualTo(2);
        assertThat(Money.zero().amount().scale()).isEqualTo(2);
    }

    @Test
    void addsDecimalsExactly() {
        Money sum = Money.of("0.1").plus(Money.of("0.2"));

        assertThat(sum).isEqualTo(Money.of("0.30"));
        assertThat(sum.amount()).isEqualByComparingTo("0.30");
    }

    @Test
    void divideDocumentsHalfEvenRounding() {
        Money result = Money.of("10").divide(3);

        assertThat(result.amount()).isEqualByComparingTo("3.33");
    }

    @Test
    void negateFlipsSign() {
        assertThat(Money.of("5").negate().amount()).isEqualByComparingTo("-5.00");
        assertThat(Money.of("-5").negate().amount()).isEqualByComparingTo("5.00");
    }

    @Test
    void signPredicates() {
        assertThat(Money.of("5").isPositive()).isTrue();
        assertThat(Money.of("-5").isNegative()).isTrue();
        assertThat(Money.zero().isZero()).isTrue();
    }

    @Test
    void multiplyRoundsHalfEven() {
        Money result = Money.of("10").multiply(new BigDecimal("0.125"));

        assertThat(result.amount()).isEqualByComparingTo("1.25");
    }

    @Test
    void compareToOrdersByAmount() {
        assertThat(Money.of("5").compareTo(Money.of("10"))).isNegative();
        assertThat(Money.of("10").compareTo(Money.of("5"))).isPositive();
        assertThat(Money.of("5").compareTo(Money.of("5.00"))).isZero();
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Money(null, Currency.UZS))
                .isInstanceOf(NullPointerException.class);
    }

}
