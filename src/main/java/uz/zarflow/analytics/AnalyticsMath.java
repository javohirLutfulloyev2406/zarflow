package uz.zarflow.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/** Pure period-math for the month endpoint — no Spring context needed to test it. */
final class AnalyticsMath {

    private AnalyticsMath() {
    }

    /** Elapsed days so far if {@code month} is the month {@code today} falls in, else the full month length. */
    static int elapsedOrFullDays(YearMonth month, LocalDate today) {
        if (!YearMonth.from(today).equals(month)) {
            return month.lengthOfMonth();
        }
        return today.getDayOfMonth();
    }

    static BigDecimal dailyAverage(BigDecimal totalExpense, int days) {
        if (days <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return totalExpense.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_EVEN);
    }

}
