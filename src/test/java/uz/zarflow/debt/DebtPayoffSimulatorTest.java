package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import uz.zarflow.debt.DebtPayoffSimulator.Input;
import uz.zarflow.debt.DebtPayoffSimulator.Result;

import static org.assertj.core.api.Assertions.assertThat;

class DebtPayoffSimulatorTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

    @Test
    void extraMonthlyOfZeroIsTheUnchangedBaseline() {
        Result result = DebtPayoffSimulator.simulate(new Input(
                bd("1000000.00"), bd("24"), InterestMethod.ANNUITY_MONTHLY, bd("100000.00"), BigDecimal.ZERO, FROM, 1));

        assertThat(result.monthsSaved()).isZero();
        assertThat(result.interestSaved()).isEqualByComparingTo("0.00");
    }

    @Test
    void moreExtraMonthlyNeverClosesTheDebtLaterAndNeverSavesLessInterest() {
        Input base = new Input(bd("2000000.00"), bd("28"), InterestMethod.ANNUITY_MONTHLY, bd("150000.00"), bd("100000.00"), FROM, 1);
        Input more = new Input(bd("2000000.00"), bd("28"), InterestMethod.ANNUITY_MONTHLY, bd("150000.00"), bd("300000.00"), FROM, 1);

        Result withSomeExtra = DebtPayoffSimulator.simulate(base);
        Result withMoreExtra = DebtPayoffSimulator.simulate(more);

        assertThat(withMoreExtra.newCloseDate()).isBeforeOrEqualTo(withSomeExtra.newCloseDate());
        assertThat(withMoreExtra.monthsSaved()).isGreaterThanOrEqualTo(withSomeExtra.monthsSaved());
        assertThat(withMoreExtra.interestSaved()).isGreaterThanOrEqualTo(withSomeExtra.interestSaved());
    }

    @Test
    void extraPaymentsShortenTheCloseDateAndReduceTotalInterest() {
        Result result = DebtPayoffSimulator.simulate(new Input(
                bd("1500000.00"), bd("30"), InterestMethod.ANNUITY_MONTHLY, bd("120000.00"), bd("200000.00"), FROM, 15));

        assertThat(result.monthsSaved()).isPositive();
        assertThat(result.interestSaved()).isPositive();
    }

    @Test
    void actual365MethodAlsoProducesAFiniteCloseDate() {
        Result result = DebtPayoffSimulator.simulate(new Input(
                bd("800000.00"), bd("22"), InterestMethod.ACTUAL_365, bd("90000.00"), bd("50000.00"), FROM, 10));

        assertThat(result.newCloseDate()).isAfter(FROM);
        assertThat(result.interestSaved()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}
