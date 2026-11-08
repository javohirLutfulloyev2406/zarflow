package uz.zarflow.debt;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import uz.zarflow.debt.PaymentSplitter.Input;
import uz.zarflow.debt.PaymentSplitter.ScheduleRow;
import uz.zarflow.debt.PaymentSplitter.Split;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSplitterTest {

    @Test
    void regularPaymentSplitsExactlyByTheMatchingScheduleRow() {
        ScheduleRow row = new ScheduleRow(bd("80000.00"), bd("20000.00"), bd("5000.00"));
        Input input = new Input(bd("105000.00"), false, bd("1000000.00"), row, bd("24"), InterestMethod.ANNUITY_MONTHLY, 30);

        Split split = PaymentSplitter.split(input);

        assertThat(split.principal()).isEqualByComparingTo("80000.00");
        assertThat(split.interest()).isEqualByComparingTo("20000.00");
        assertThat(split.fee()).isEqualByComparingTo("5000.00");
    }

    @Test
    void regularPaymentWithNoScheduleRowSplitsByTheDebtsOwnMethod() {
        // ANNUITY_MONTHLY, 24%/year on 1 000 000 outstanding -> 2% monthly = 20 000 interest.
        Input input = new Input(bd("120000.00"), false, bd("1000000.00"), null, bd("24"), InterestMethod.ANNUITY_MONTHLY, 30);

        Split split = PaymentSplitter.split(input);

        assertThat(split.interest()).isEqualByComparingTo("20000.00");
        assertThat(split.principal()).isEqualByComparingTo("100000.00");
        assertThat(split.fee()).isEqualByComparingTo("0.00");
    }

    @Test
    void extraPaymentIsAlwaysOneHundredPercentPrincipal() {
        Input input = new Input(bd("250000.00"), true, bd("1000000.00"), null, bd("24"), InterestMethod.ANNUITY_MONTHLY, 30);

        Split split = PaymentSplitter.split(input);

        assertThat(split.principal()).isEqualByComparingTo("250000.00");
        assertThat(split.interest()).isEqualByComparingTo("0.00");
        assertThat(split.fee()).isEqualByComparingTo("0.00");
    }

    @Test
    void overpaymentPrincipalIsCappedAtWhateverIsStillOutstanding() {
        // Extra payment of 500 000 but only 300 000 is actually owed.
        Input input = new Input(bd("500000.00"), true, bd("300000.00"), null, bd("24"), InterestMethod.ANNUITY_MONTHLY, 30);

        Split split = PaymentSplitter.split(input);

        assertThat(split.principal()).isEqualByComparingTo("300000.00");
    }

    @Test
    void aScheduleRowPrincipalExceedingOutstandingIsAlsoCapped() {
        // Defensive: even a stale/mismatched schedule row's principal is capped so outstanding
        // can never go negative.
        ScheduleRow row = new ScheduleRow(bd("90000.00"), bd("20000.00"), bd("0.00"));
        Input input = new Input(bd("110000.00"), false, bd("50000.00"), row, bd("24"), InterestMethod.ANNUITY_MONTHLY, 30);

        Split split = PaymentSplitter.split(input);

        assertThat(split.principal()).isEqualByComparingTo("50000.00");
    }

    @Test
    void payingExactlyTheOutstandingPrincipalClosesTheDebtAtZero() {
        ScheduleRow row = new ScheduleRow(bd("50000.00"), bd("0.00"), bd("0.00"));
        Input input = new Input(bd("50000.00"), false, bd("50000.00"), row, bd("0"), InterestMethod.ANNUITY_MONTHLY, 30);

        Split split = PaymentSplitter.split(input);
        BigDecimal remaining = bd("50000.00").subtract(split.principal());

        assertThat(remaining).isEqualByComparingTo("0.00");
    }

    @Test
    void actual365UsesActualDaysInThePeriodWhenSplittingByMethod() {
        // 24%/year on 1 000 000, 31-day period: 1000000*0.24*31/365 = 20 383.56
        Input input = new Input(bd("50000.00"), false, bd("1000000.00"), null, bd("24"), InterestMethod.ACTUAL_365, 31);

        Split split = PaymentSplitter.split(input);

        assertThat(split.interest()).isEqualByComparingTo("20383.56");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}
