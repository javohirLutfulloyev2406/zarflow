package uz.zarflow.savings;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.savings.LeftoverCalculator.Entry;
import uz.zarflow.savings.LeftoverCalculator.Input;
import uz.zarflow.savings.LeftoverCalculator.Result;
import uz.zarflow.transaction.TransactionType;

import static org.assertj.core.api.Assertions.assertThat;

class LeftoverCalculatorTest {

    @Test
    void positiveLeftoverWhenSpendingIsUnderTheFreeBudget() {
        Result result = LeftoverCalculator.calculate(new Input(bd("1000000"), List.of(
                new Entry(TransactionType.EXPENSE, bd("600000"), false))));

        assertThat(result.positive()).isTrue();
        assertThat(result.leftover()).isEqualByComparingTo("400000.00");
    }

    @Test
    void exactlyZeroLeftoverIsNotPositive() {
        Result result = LeftoverCalculator.calculate(new Input(bd("1000000"), List.of(
                new Entry(TransactionType.EXPENSE, bd("1000000"), false))));

        assertThat(result.positive()).isFalse();
        assertThat(result.leftover()).isEqualByComparingTo("0.00");
    }

    @Test
    void negativeLeftoverIsReportedAsZeroNotNegative() {
        Result result = LeftoverCalculator.calculate(new Input(bd("1000000"), List.of(
                new Entry(TransactionType.EXPENSE, bd("1500000"), false))));

        assertThat(result.positive()).isFalse();
        assertThat(result.leftover()).isEqualByComparingTo("0.00");
    }

    @Test
    void transferRowsNeverReduceTheLeftover() {
        Result result = LeftoverCalculator.calculate(new Input(bd("1000000"), List.of(
                new Entry(TransactionType.EXPENSE, bd("300000"), false),
                new Entry(TransactionType.TRANSFER, bd("5000000"), false))));

        assertThat(result.leftover()).isEqualByComparingTo("700000.00");
    }

    @Test
    void fixedExpenseRowsNeverReduceTheLeftover() {
        // Fixed obligations are already accounted for inside freeBudget itself -- counting
        // them again here would double-subtract them.
        Result result = LeftoverCalculator.calculate(new Input(bd("1000000"), List.of(
                new Entry(TransactionType.EXPENSE, bd("300000"), false),
                new Entry(TransactionType.EXPENSE, bd("200000"), true))));

        assertThat(result.leftover()).isEqualByComparingTo("700000.00");
    }

    @Test
    void debtPaymentRowsNeverReduceTheLeftoverEither() {
        Result result = LeftoverCalculator.calculate(new Input(bd("1000000"), List.of(
                new Entry(TransactionType.EXPENSE, bd("300000"), false),
                new Entry(TransactionType.DEBT_PAYMENT, bd("400000"), false))));

        assertThat(result.leftover()).isEqualByComparingTo("700000.00");
    }

    @Test
    void incomeRowsDoNotAffectTheLeftoverEither() {
        Result result = LeftoverCalculator.calculate(new Input(bd("1000000"), List.of(
                new Entry(TransactionType.EXPENSE, bd("300000"), false),
                new Entry(TransactionType.INCOME, bd("2000000"), false))));

        assertThat(result.leftover()).isEqualByComparingTo("700000.00");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}
