package uz.zarflow.shared.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Percent-change and percent-share math shared by analytics (month breakdown) and
 * the bot's /week and /month reports. {@code null} means "not comparable" (no
 * previous-period baseline) rather than a misleading 0% or 100%.
 */
public final class Percentages {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Percentages() {
    }

    public static BigDecimal changeOf(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_EVEN)
                .multiply(HUNDRED)
                .setScale(1, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal shareOf(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return part.divide(total, 4, RoundingMode.HALF_EVEN)
                .multiply(HUNDRED)
                .setScale(1, RoundingMode.HALF_EVEN);
    }

}
