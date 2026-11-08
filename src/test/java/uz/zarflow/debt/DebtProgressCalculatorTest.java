package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.debt.DebtProgressCalculator.Input;
import uz.zarflow.debt.DebtProgressCalculator.Result;
import uz.zarflow.debt.DebtProgressCalculator.Row;

import static org.assertj.core.api.Assertions.assertThat;

class DebtProgressCalculatorTest {

    @Test
    void paidPercentReflectsHowMuchPrincipalHasAlreadyBeenPaidDown() {
        Result result = DebtProgressCalculator.calculate(new Input(
                bd("1000000.00"), bd("750000.00"), List.of()));

        assertThat(result.paidPercent()).isEqualByComparingTo("25.00");
    }

    @Test
    void monthsLeftAndCloseDateComeFromTheRemainingScheduleRows() {
        List<Row> remaining = List.of(
                new Row(LocalDate.of(2026, 12, 1), bd("5000.00"), bd("0.00")),
                new Row(LocalDate.of(2027, 1, 1), bd("3000.00"), bd("0.00")),
                new Row(LocalDate.of(2027, 2, 1), bd("1000.00"), bd("500.00")));

        Result result = DebtProgressCalculator.calculate(new Input(bd("900000.00"), bd("300000.00"), remaining));

        assertThat(result.monthsLeft()).isEqualTo(3);
        assertThat(result.estimatedCloseDate()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(result.interestRemaining()).isEqualByComparingTo("9500.00");
    }

    @Test
    void anEmptyRemainingScheduleMeansZeroMonthsLeftAndNoCloseDate() {
        Result result = DebtProgressCalculator.calculate(new Input(bd("500000.00"), bd("0.00"), List.of()));

        assertThat(result.monthsLeft()).isZero();
        assertThat(result.estimatedCloseDate()).isNull();
        assertThat(result.interestRemaining()).isEqualByComparingTo("0.00");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}
