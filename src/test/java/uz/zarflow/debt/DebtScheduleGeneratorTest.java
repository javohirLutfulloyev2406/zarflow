package uz.zarflow.debt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.debt.DebtScheduleGenerator.Input;
import uz.zarflow.debt.DebtScheduleGenerator.Result;
import uz.zarflow.debt.DebtScheduleGenerator.Row;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DebtScheduleGeneratorTest {

    @Test
    void textbookAnnuityExampleMatchesTheStandardFormula() {
        BigDecimal principal = BigDecimal.valueOf(1_200_000);
        BigDecimal annualRatePercent = BigDecimal.valueOf(24);
        int term = 12;

        Result result = DebtScheduleGenerator.generate(new Input(
                principal, annualRatePercent, InterestMethod.ANNUITY_MONTHLY, term, LocalDate.of(2026, 1, 15), 15));

        double r = 0.24 / 12;
        double expectedPayment = principal.doubleValue() * r / (1 - Math.pow(1 + r, -term));
        assertThat(result.monthlyPayment().doubleValue()).isCloseTo(expectedPayment, within(1.0));

        Row first = result.rows().get(0);
        assertThat(first.interest().doubleValue()).isCloseTo(principal.doubleValue() * r, within(1.0));
        assertThat(first.principal().add(first.interest()).doubleValue()).isCloseTo(expectedPayment, within(1.0));
    }

    @Test
    void sumOfPrincipalAlwaysEqualsTheOriginalAmount() {
        Result result = DebtScheduleGenerator.generate(new Input(
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(18), InterestMethod.ANNUITY_MONTHLY, 11,
                LocalDate.of(2026, 3, 10), 10));

        BigDecimal sum = result.rows().stream().map(Row::principal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1000000.00");
    }

    @Test
    void lastRowAbsorbsTheRoundingRemainderSoOutstandingEndsAtExactlyZero() {
        // 1 000 000 / 3 does not divide evenly -- the classic rounding-drift case.
        Result result = DebtScheduleGenerator.generate(new Input(
                BigDecimal.valueOf(1_000_000), BigDecimal.ZERO, InterestMethod.ANNUITY_MONTHLY, 3,
                LocalDate.of(2026, 1, 1), 1));

        List<Row> rows = result.rows();
        BigDecimal outstanding = BigDecimal.valueOf(1_000_000);
        for (Row row : rows) {
            outstanding = outstanding.subtract(row.principal());
        }
        assertThat(outstanding).isEqualByComparingTo("0.00");
        // The first two rows are identical (333333.33); the last one picks up the odd cent.
        assertThat(rows.get(0).principal()).isEqualByComparingTo(rows.get(1).principal());
        assertThat(rows.get(2).principal()).isNotEqualByComparingTo(rows.get(0).principal());
    }

    @Test
    void zeroPercentLoanIsEqualPrincipalWithNoInterest() {
        Result result = DebtScheduleGenerator.generate(new Input(
                BigDecimal.valueOf(900_000), BigDecimal.ZERO, InterestMethod.ANNUITY_MONTHLY, 9,
                LocalDate.of(2026, 5, 1), 1));

        assertThat(result.rows()).allSatisfy(row -> assertThat(row.interest()).isEqualByComparingTo("0.00"));
        for (int i = 0; i < result.rows().size() - 1; i++) {
            assertThat(result.rows().get(i).principal()).isEqualByComparingTo("100000.00");
        }
    }

    @Test
    void actual365ChargesInterestProportionalToOutstandingAndActualDaysInThePeriod() {
        // Due dates: Feb 10 (31-day January period), Mar 10 (28-day February period, non-leap).
        BigDecimal originalAmount = BigDecimal.valueOf(5_000_000);
        BigDecimal annualRatePercent = BigDecimal.valueOf(24);
        Result result = DebtScheduleGenerator.generate(new Input(
                originalAmount, annualRatePercent, InterestMethod.ACTUAL_365, 6, LocalDate.of(2027, 1, 10), 10));

        Row january = result.rows().get(0);
        Row february = result.rows().get(1);
        BigDecimal outstandingEnteringFebruary = originalAmount.subtract(january.principal());

        double rate = 0.24;
        double expectedJanuaryInterest = originalAmount.doubleValue() * rate * 31 / 365;
        double expectedFebruaryInterest = outstandingEnteringFebruary.doubleValue() * rate * 28 / 365;

        assertThat(january.interest().doubleValue()).isCloseTo(expectedJanuaryInterest, within(1.0));
        assertThat(february.interest().doubleValue()).isCloseTo(expectedFebruaryInterest, within(1.0));
        // Fewer days AND a smaller balance both push February's interest below January's.
        assertThat(february.interest()).isLessThan(january.interest());
    }

    @Test
    void paymentDay31ClampsToTheLastDayOfShorterMonths() {
        Result result = DebtScheduleGenerator.generate(new Input(
                BigDecimal.valueOf(300_000), BigDecimal.ZERO, InterestMethod.ANNUITY_MONTHLY, 3,
                LocalDate.of(2027, 1, 31), 31));

        assertThat(result.rows().get(0).dueDate()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(result.rows().get(1).dueDate()).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(result.rows().get(2).dueDate()).isEqualTo(LocalDate.of(2027, 4, 30));
    }

}
