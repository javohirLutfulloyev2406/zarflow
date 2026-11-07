package uz.zarflow.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsMathTest {

    @Test
    void elapsedOrFullDaysUsesElapsedDaysForCurrentMonth() {
        int days = AnalyticsMath.elapsedOrFullDays(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 7));

        assertThat(days).isEqualTo(7);
    }

    @Test
    void elapsedOrFullDaysUsesFullMonthLengthForPastMonth() {
        int days = AnalyticsMath.elapsedOrFullDays(YearMonth.of(2026, 2), LocalDate.of(2026, 11, 7));

        assertThat(days).isEqualTo(28);
    }

    @Test
    void elapsedOrFullDaysHandlesLeapFebruary() {
        int days = AnalyticsMath.elapsedOrFullDays(YearMonth.of(2028, 2), LocalDate.of(2028, 11, 7));

        assertThat(days).isEqualTo(29);
    }

    @Test
    void dailyAverageDividesExpenseByDays() {
        BigDecimal average = AnalyticsMath.dailyAverage(new BigDecimal("700000.00"), 7);

        assertThat(average).isEqualByComparingTo("100000.00");
    }

    @Test
    void dailyAverageIsZeroWhenNoDaysElapsed() {
        BigDecimal average = AnalyticsMath.dailyAverage(new BigDecimal("500000.00"), 0);

        assertThat(average).isEqualByComparingTo("0.00");
    }

}
