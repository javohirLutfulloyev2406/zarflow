package uz.zarflow.savings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import uz.zarflow.savings.SavingsGoalCalculator.Input;
import uz.zarflow.savings.SavingsGoalCalculator.Result;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsGoalCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    @Test
    void noTargetDateMeansNoPerMonthOrPerWeekFigures() {
        Result result = calculate(bd("5000000"), bd("1000000"), null, List.of(bd("200000")));

        assertThat(result.perMonth()).isNull();
        assertThat(result.perWeek()).isNull();
        // Without a deadline, saving anything at all counts as "on track".
        assertThat(result.onTrack()).isTrue();
    }

    @Test
    void pastTargetDateNeedsTheWholeRemainingAmountRightNow() {
        Result result = calculate(bd("5000000"), bd("1000000"), TODAY.minusDays(10), List.of(bd("200000")));

        assertThat(result.perMonth()).isEqualByComparingTo(result.remaining());
        assertThat(result.perWeek()).isEqualByComparingTo(result.remaining());
        assertThat(result.onTrack()).isFalse();
    }

    @Test
    void zeroPaceHasNoForecastDateAndIsNotOnTrackWithADeadline() {
        Result result = calculate(bd("5000000"), bd("1000000"), TODAY.plusMonths(6), List.of(BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(result.pace()).isEqualByComparingTo("0.00");
        assertThat(result.forecastDate()).isNull();
        assertThat(result.onTrack()).isFalse();
    }

    @Test
    void alreadyAchievedMeansZeroRemainingAndOnTrackRegardlessOfPace() {
        Result result = calculate(bd("1000000"), bd("1000000"), TODAY.plusMonths(1), List.of());

        assertThat(result.achieved()).isTrue();
        assertThat(result.remaining()).isEqualByComparingTo("0.00");
        assertThat(result.onTrack()).isTrue();
        assertThat(result.forecastDate()).isEqualTo(TODAY);
    }

    @Test
    void savingMoreThanTheTargetStillReportsAchievedWithZeroRemaining() {
        Result result = calculate(bd("1000000"), bd("1500000"), null, List.of());

        assertThat(result.achieved()).isTrue();
        assertThat(result.remaining()).isEqualByComparingTo("0.00");
    }

    @Test
    void perMonthAndPerWeekRoundUpToTheNearestWholeSom() {
        // remaining = 1 000 000, target date exactly 2 months (60 days) out -> 500 000 exactly
        // divides evenly for months, but weeks (ceil(60/7)=9) does not: 1 000 000 / 9 = 111111.11...
        Result result = calculate(bd("2000000"), bd("1000000"), TODAY.plusDays(60), List.of(bd("100000")));

        assertThat(result.perWeek()).isEqualByComparingTo("111112.00");
        // Rounded-up whole-so'm result, re-multiplied, must cover the full remaining amount.
        assertThat(result.perWeek().multiply(BigDecimal.valueOf(9))).isGreaterThanOrEqualTo(result.remaining());
    }

    @Test
    void forecastDateProjectsForwardFromTodayAtTheAveragePace() {
        // remaining = 900 000, pace = 300 000/month -> exactly 3 months out.
        Result result = calculate(bd("1200000"), bd("300000"), null, List.of(bd("300000"), bd("300000"), bd("300000")));

        assertThat(result.forecastDate()).isEqualTo(TODAY.plusMonths(3));
    }

    @Test
    void onTrackWhenForecastArrivesOnOrBeforeTheTargetDate() {
        Result onTime = calculate(bd("1200000"), bd("300000"), TODAY.plusMonths(3), List.of(bd("300000"), bd("300000"), bd("300000")));
        assertThat(onTime.onTrack()).isTrue();

        Result late = calculate(bd("1200000"), bd("300000"), TODAY.plusMonths(2), List.of(bd("300000"), bd("300000"), bd("300000")));
        assertThat(late.onTrack()).isFalse();
    }

    private static Result calculate(BigDecimal target, BigDecimal saved, LocalDate targetDate, List<BigDecimal> recent) {
        return SavingsGoalCalculator.calculate(new Input(target, saved, targetDate, TODAY, recent));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}
