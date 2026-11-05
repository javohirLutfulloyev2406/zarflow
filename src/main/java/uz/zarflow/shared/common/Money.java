package uz.zarflow.shared.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable money value. Always scale 2, HALF_EVEN rounding. UZS only for now —
 * {@link #plus}/{@link #minus}/{@link #compareTo} guard against a currency mismatch
 * so the check is already in place once a second currency exists.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount), Currency.UZS);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.UZS);
    }

    public static Money ofWhole(long som) {
        return new Money(BigDecimal.valueOf(som), Currency.UZS);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.UZS);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor).setScale(2, RoundingMode.HALF_EVEN), currency);
    }

    public Money divide(int parts) {
        return new Money(amount.divide(BigDecimal.valueOf(parts), 2, RoundingMode.HALF_EVEN), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

}
