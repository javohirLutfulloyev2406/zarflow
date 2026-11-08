package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import uz.zarflow.debt.DebtPayoffStrategy.Comparison;
import uz.zarflow.debt.DebtPayoffStrategy.DebtInput;
import uz.zarflow.debt.DebtPayoffStrategy.Strategy;

import static org.assertj.core.api.Assertions.assertThat;

class DebtPayoffStrategyTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

    @Test
    void avalancheNeverCostsMoreTotalInterestThanSnowball() {
        // Debt A: high rate, high balance (avalanche targets it first; snowball targets it last).
        // Debt B: low rate, low balance (snowball targets it first; avalanche targets it last).
        DebtInput debtA = new DebtInput(
                UUID.randomUUID(), bd("2000000.00"), bd("36"), InterestMethod.ANNUITY_MONTHLY, bd("100000.00"), 5, FROM);
        DebtInput debtB = new DebtInput(
                UUID.randomUUID(), bd("500000.00"), bd("8"), InterestMethod.ANNUITY_MONTHLY, bd("50000.00"), 15, FROM);

        Comparison comparison = DebtPayoffStrategy.compare(List.of(debtA, debtB), bd("150000.00"));

        assertThat(comparison.avalanche().totalInterest()).isLessThanOrEqualTo(comparison.snowball().totalInterest());
        assertThat(comparison.recommended()).isEqualTo(Strategy.AVALANCHE);
    }

    @Test
    void bothStrategiesEventuallyCloseAllDebts() {
        DebtInput debtA = new DebtInput(
                UUID.randomUUID(), bd("1000000.00"), bd("20"), InterestMethod.ANNUITY_MONTHLY, bd("80000.00"), 1, FROM);
        DebtInput debtB = new DebtInput(
                UUID.randomUUID(), bd("600000.00"), bd("15"), InterestMethod.ANNUITY_MONTHLY, bd("60000.00"), 10, FROM);

        Comparison comparison = DebtPayoffStrategy.compare(List.of(debtA, debtB), bd("50000.00"));

        assertThat(comparison.avalanche().closeDate()).isAfter(FROM);
        assertThat(comparison.snowball().closeDate()).isAfter(FROM);
        assertThat(comparison.avalanche().totalInterest()).isPositive();
        assertThat(comparison.snowball().totalInterest()).isPositive();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}
